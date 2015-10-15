package org.redpill.alfresco.ldap.behaviour;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.springframework.util.Assert;

/**
 * Attach additional information to the person object
 * 
 */
public class PersonPolicy extends AbstractPolicy implements OnCreateNodePolicy, OnUpdatePropertiesPolicy {

  private static final Logger LOG = Logger.getLogger(PersonPolicy.class);
  private static Boolean initialized = false;

  protected LdapUserService ldapUserService;
  protected AuthorityService authorityService;
  protected String syncZoneId;

  @Override
  public void onCreateNode(final ChildAssociationRef childAssocRef) {
    LOG.trace("onCreateNode begin");
    final NodeRef nodeRef = childAssocRef.getChildRef();

    if (!shouldSkipCreatePolicy(nodeRef)) {
      addUserToLdap(nodeRef);
    }
    LOG.trace("onCreateNode end");
  }

  protected void addUserToLdap(NodeRef nodeRef) {
    Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
    final String userId = (String) properties.get(ContentModel.PROP_USERNAME);
    String password = "{MD4}" + (String) properties.get(ContentModel.PROP_PASSWORD);
    // LOG.error("PASSWORD: " + password);
    String email = (String) properties.get(ContentModel.PROP_EMAIL);
    if (email == null) {
      email = "";
    }
    String firstName = (String) properties.get(ContentModel.PROP_FIRSTNAME);
    String lastName = (String) properties.get(ContentModel.PROP_LASTNAME);
    ldapUserService.createUser(userId, password, true, email, firstName, lastName);

    // Add user to zone
    final String zoneName = AuthorityService.ZONE_AUTH_EXT_PREFIX + syncZoneId;
    final Set<String> zones = new HashSet<String>();
    zones.add(zoneName);

    AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

      @Override
      public Void doWork() throws Exception {

        authorityService.getOrCreateZone(zoneName);
        authorityService.addAuthorityToZones(userId, zones);

        return null;
      }
    });

    if (LOG.isInfoEnabled()) {
      LOG.info("Adding " + userId + " to zone " + zoneName);
    }
  }

  @Override
  public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
    LOG.trace("onUpdateProperties begin");

    if (!shouldSkipUpdatePropertiesPolicy(nodeRef, before, after)) {
      updateUserInLdap(nodeRef, after);
    }
    LOG.trace("onUpdateProperties end");
  }

  protected void updateUserInLdap(NodeRef nodeRef, Map<QName, Serializable> after) {
    ldapUserService.editUser((String) after.get(ContentModel.PROP_USERNAME), null, null, (String) after.get(ContentModel.PROP_EMAIL), (String) after.get(ContentModel.PROP_FIRSTNAME),
        (String) after.get(ContentModel.PROP_LASTNAME));
  }

  private boolean shouldSkipUpdatePropertiesPolicy(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
    boolean skip = super.shouldSkipPolicy(nodeRef);
    if (!skip) {
      if (!propertyChanged(before, after, ContentModel.PROP_EMAIL) && !propertyChanged(before, after, ContentModel.PROP_FIRSTNAME) && !propertyChanged(before, after, ContentModel.PROP_LASTNAME)) {
        LOG.trace("No ldap properties updated. Skipping property update in ldap.");
        skip = true;
      } else {
        String userId = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME);
        Set<String> authorityZones = authorityService.getAuthorityZones(userId);
        if (!authorityZones.contains("AUTH.EXT." + syncZoneId)) {
          LOG.trace("User is not part of " + "AUTH.EXT." + syncZoneId + " zone. Skipping property update in ldap.");
          skip = true;
        }
      }
    }
    return skip;
  }

  protected boolean propertyChanged(Map<QName, Serializable> before, Map<QName, Serializable> after, QName property) {
    Serializable a = (before == null) ? null : before.get(property);
    Serializable b = (after == null) ? null : after.get(property);
    return ((a != null && !a.equals(b)) || (a == null && b != null));
  }

  protected boolean shouldSkipCreatePolicy(NodeRef nodeRef) {
    boolean result = super.shouldSkipPolicy(nodeRef);
    if (!result) {
      String userId = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME);
      Set<String> authorityZones = authorityService.getAuthorityZones(userId);

      for (String authorityZone : authorityZones) {
        // Skip if user comes from an external zone such as an LDAP
        if (authorityZone.startsWith(AuthorityService.ZONE_AUTH_EXT_PREFIX)) {
          if (LOG.isTraceEnabled()) {
            LOG.trace("User " + userId + " is originating from an external zone already. Will not move to LDAP.");
          }
          result = true;
          continue;
        }
      }

      if (AuthenticationUtil.getAdminUserName().equals(userId)) {
        LOG.info("Skipping admin user. Will not move to LDAP.");
        result = true;
      }
      if (AuthenticationUtil.getSystemUserName().equals(userId) || (AuthenticationUtil.getSystemUserName() + "User").equals(userId)) {
        LOG.info("Skipping sytem user. Will not move to LDAP.");
        result = true;
      }
    }
    return result;
  }

  public void setAuthorityService(AuthorityService authorityService) {
    this.authorityService = authorityService;
  }

  public void setLdapUserService(LdapUserService ldapUserService) {
    this.ldapUserService = ldapUserService;
  }

  public void setSyncZoneId(String syncZoneId) {
    this.syncZoneId = syncZoneId;
  }

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    Assert.notNull(authorityService);
    Assert.notNull(ldapUserService);
    Assert.notNull(syncZoneId);
    if (!initialized) {
      LOG.info("Initialized policy");
      policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_PERSON, new JavaBehaviour(this, "onCreateNode", NotificationFrequency.TRANSACTION_COMMIT));

      initialized = true;
    }
  }

}
