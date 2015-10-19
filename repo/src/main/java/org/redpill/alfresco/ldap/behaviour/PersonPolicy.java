package org.redpill.alfresco.ldap.behaviour;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.log4j.Logger;
import org.redpill.alfresco.ldap.model.RlLdapModel;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.springframework.util.Assert;

/**
 * Attach additional information to the person object
 * 
 */
public class PersonPolicy extends AbstractPolicy implements OnCreateNodePolicy, OnUpdatePropertiesPolicy, OnUpdateNodePolicy {

  private static final Logger LOG = Logger.getLogger(PersonPolicy.class);

  private static final StoreRef STOREREF_USERS = new StoreRef("user", "alfrescoUserStore");

  private static Boolean initialized = false;

  protected LdapUserService ldapUserService;
  protected AuthorityService authorityService;
  protected TenantService tenantService;
  protected NamespacePrefixResolver namespacePrefixResolver;

  protected String syncZoneId;
  protected boolean enabled;

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
    String email = (String) properties.get(ContentModel.PROP_EMAIL);
    if (email == null) {
      email = "";
    }
    String firstName = (String) properties.get(ContentModel.PROP_FIRSTNAME);
    String lastName = (String) properties.get(ContentModel.PROP_LASTNAME);

    NodeRef userInUserStoreNodeRef = getUserOrNull(userId);
    if (userInUserStoreNodeRef != null && nodeService.hasAspect(userInUserStoreNodeRef, RlLdapModel.ASPECT_TEMPORARY_PASSWORD)) {
      password = (String) nodeService.getProperty(userInUserStoreNodeRef, RlLdapModel.PROP_TEMPORARY_PASSWORD);
      ldapUserService.createUser(userId, password, false, email, firstName, lastName);
      boolean enabled = behaviourFilter.isEnabled(userInUserStoreNodeRef);
      if (enabled)
        behaviourFilter.disableBehaviour(userInUserStoreNodeRef);
      // Remove the aspect and its properties
      LOG.trace("Removing temporary password aspect for user " + userId);
      nodeService.removeAspect(userInUserStoreNodeRef, RlLdapModel.ASPECT_TEMPORARY_PASSWORD);
      if (enabled)
        behaviourFilter.enableBehaviour(userInUserStoreNodeRef);
    } else {
      ldapUserService.createUser(userId, password, true, email, firstName, lastName);
    }

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
    if (!enabled) {
      LOG.info("Skipping policy. LDAP Manager is disabled.");
      skip = true;
    }
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
    if (!enabled) {
      LOG.info("Skipping policy. LDAP Manager is disabled.");
      result = true;
    }
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

  protected NodeRef getUserOrNull(final String caseSensitiveSearchUserName) {
    return AuthenticationUtil.runAsSystem(new RunAsWork<NodeRef>() {

      @Override
      public NodeRef doWork() throws Exception {
        List<ChildAssociationRef> results = nodeService.getChildAssocs(getUserFolderLocation(caseSensitiveSearchUserName), ContentModel.ASSOC_CHILDREN,
            QName.createQName(ContentModel.USER_MODEL_URI, caseSensitiveSearchUserName));
        if (!results.isEmpty()) {
          // Extract values from the query results
          NodeRef userRef = tenantService.getName(results.get(0).getChildRef());
          return userRef;
        }
        return null;
      }
    });
    
  }

  private NodeRef getUserFolderLocation(String caseSensitiveUserName) {
    NodeRef userNodeRef = null;
    QName qnameAssocSystem = QName.createQName("sys", "system", namespacePrefixResolver);
    QName qnameAssocUsers = QName.createQName("sys", "people", namespacePrefixResolver);

    // StoreRef userStoreRef = tenantService.getName(caseSensitiveUserName, new
    // StoreRef(STOREREF_USERS.getProtocol(), STOREREF_USERS.getIdentifier()));
    StoreRef userStoreRef = new StoreRef(STOREREF_USERS.getProtocol(), STOREREF_USERS.getIdentifier());

    // AR-527
    try {
      NodeRef rootNode = nodeService.getRootNode(userStoreRef);
      List<ChildAssociationRef> results = nodeService.getChildAssocs(rootNode, RegexQNamePattern.MATCH_ALL, qnameAssocSystem);
      NodeRef sysNodeRef = null;
      if (results.size() == 0) {
        throw new AlfrescoRuntimeException("Required authority system folder path not found: " + qnameAssocSystem);
      } else {
        sysNodeRef = results.get(0).getChildRef();
      }
      results = nodeService.getChildAssocs(sysNodeRef, RegexQNamePattern.MATCH_ALL, qnameAssocUsers);
      if (results.size() == 0) {
        throw new AlfrescoRuntimeException("Required user folder path not found: " + qnameAssocUsers);
      } else {
        userNodeRef = tenantService.getName(results.get(0).getChildRef());
      }
    } catch (Exception e) {
      LOG.error("Error while getting user folder location", e);
    }

    return userNodeRef;
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

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setTenantService(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  public void setNamespacePrefixResolver(NamespacePrefixResolver namespacePrefixResolver) {
    this.namespacePrefixResolver = namespacePrefixResolver;
  }

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    Assert.notNull(authorityService);
    Assert.notNull(ldapUserService);
    Assert.notNull(syncZoneId);
    Assert.notNull(tenantService);
    Assert.notNull(namespacePrefixResolver);
    if (!initialized) {
      LOG.info("Initialized policy");
      policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_PERSON, new JavaBehaviour(this, "onCreateNode", NotificationFrequency.TRANSACTION_COMMIT));
      policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, ContentModel.TYPE_PERSON, new JavaBehaviour(this, "onUpdateProperties", NotificationFrequency.TRANSACTION_COMMIT));
      policyComponent.bindClassBehaviour(OnUpdateNodePolicy.QNAME, ContentModel.TYPE_PERSON, new JavaBehaviour(this, "onUpdateNode", NotificationFrequency.TRANSACTION_COMMIT));

      initialized = true;
    }
  }

  @Override
  public void onUpdateNode(NodeRef nodeRef) {
    LOG.trace("onUpdateNode begin");
    LOG.trace("onUpdateNode end");
  }

}
