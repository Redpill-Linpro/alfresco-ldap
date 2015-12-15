package org.redpill.alfresco.ldap.security.authentication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.RepositoryAuthenticationDao;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.redpill.alfresco.ldap.model.RlLdapModel;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.springframework.util.Assert;

public class CustomRepositoryAuthenticationDao extends RepositoryAuthenticationDao {

  private static final Logger LOG = Logger.getLogger(CustomRepositoryAuthenticationDao.class);

  protected String syncZoneId;

  protected LdapUserService ldapUserService;
  protected AuthorityService authorityService;
  protected BehaviourFilter behaviourFilter;
  protected TransactionService transactionService;

  @Override
  public void setTransactionService(TransactionService transactionService) {
    super.setTransactionService(transactionService);
    this.transactionService = transactionService;
  }

  public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
    this.behaviourFilter = behaviourFilter;
  }

  protected boolean shouldSkipCreate(NodeRef nodeRef, String userId) {
    boolean skip = false;

    if (!skip) {
      // String userId = (String) nodeService.getProperty(nodeRef,
      // ContentModel.PROP_USERNAME);
      Set<String> authorityZones = authorityService.getAuthorityZones(userId);
      if (authorityZones != null) {
        for (String authorityZone : authorityZones) {
          // Skip if user comes from an external zone such as an LDAP
          if (authorityZone.startsWith(AuthorityService.ZONE_AUTH_EXT_PREFIX)) {
            if (LOG.isTraceEnabled()) {
              LOG.trace("User " + userId + " is originating from an external zone already. Will not move to LDAP.");
            }
            skip = true;
            continue;
          }
        }
      }
      if (AuthenticationUtil.getAdminUserName().equals(userId)) {
        LOG.info("Skipping admin user. Will not move to LDAP.");
        skip = true;
      }
      if (AuthenticationUtil.getSystemUserName().equals(userId) || (AuthenticationUtil.getSystemUserName() + "User").equals(userId)) {
        LOG.info("Skipping sytem user. Will not move to LDAP.");
        skip = true;
      }
    }
    return skip;
  }

  @Override
  public void createUser(final String caseSensitiveUserName, final char[] rawPassword) throws AuthenticationException {
    super.createUser(caseSensitiveUserName, rawPassword);
    final NodeRef userRef = getUserOrNull(caseSensitiveUserName);
    if (userRef != null) {
      Set<String> authorityZones = authorityService.getAuthorityZones(caseSensitiveUserName);
      if (authorityZones != null && authorityZones.contains("AUTH.EXT." + syncZoneId)) {
        LOG.warn("user already in zone");
      }
      if (!shouldSkipCreate(userRef, caseSensitiveUserName)) {

        boolean enabled = behaviourFilter.isEnabled(userRef);
        if (enabled)
          behaviourFilter.disableBehaviour(userRef);
        String password = String.copyValueOf(rawPassword);
        Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>();
        LOG.trace("Adding temporary password aspect for user " + caseSensitiveUserName);
        aspectProperties.put(RlLdapModel.PROP_TEMPORARY_PASSWORD, password);
        nodeService.addAspect(userRef, RlLdapModel.ASPECT_TEMPORARY_PASSWORD, aspectProperties);
        if (enabled)
          behaviourFilter.enableBehaviour(userRef);

      }
    }
    /*
     * tenantService.checkDomainUser(caseSensitiveUserName);
     * 
     * NodeRef userRef = getUserOrNull(caseSensitiveUserName); if (userRef !=
     * null) { throw new AuthenticationException("User already exists: " +
     * caseSensitiveUserName); } NodeRef typesNode =
     * getUserFolderLocation(caseSensitiveUserName); Map<QName, Serializable>
     * properties = new HashMap<QName, Serializable>();
     * properties.put(ContentModel.PROP_USER_USERNAME, caseSensitiveUserName);
     * String salt = GUID.generate(); properties.put(ContentModel.PROP_SALT,
     * salt); properties.put(ContentModel.PROP_PASSWORD,
     * passwordEncoder.encodePassword(new String(rawPassword), null));
     * properties.put(ContentModel.PROP_PASSWORD_SHA256,
     * sha256PasswordEncoder.encodePassword(new String(rawPassword), salt));
     * properties.put(ContentModel.PROP_ACCOUNT_EXPIRES,
     * Boolean.valueOf(false));
     * properties.put(ContentModel.PROP_CREDENTIALS_EXPIRE,
     * Boolean.valueOf(false)); properties.put(ContentModel.PROP_ENABLED,
     * Boolean.valueOf(true)); properties.put(ContentModel.PROP_ACCOUNT_LOCKED,
     * Boolean.valueOf(false)); nodeService.createNode(typesNode,
     * ContentModel.ASSOC_CHILDREN,
     * QName.createQName(ContentModel.USER_MODEL_URI, caseSensitiveUserName),
     * ContentModel.TYPE_USER, properties);
     */
  }

  /*
   * @Override public void updateUser(String userName, char[] rawPassword)
   * throws AuthenticationException { // get all the authority zones for the
   * user Set<String> authorityZones =
   * authorityService.getAuthorityZones(userName);
   * 
   * if (authorityZones.contains("AUTH.EXT." + syncZoneId)) { // if the user is
   * in the configured ${ldapMgr.syncZoneId} then it's an // LDAP user
   * ldapUserService.changePassword(userName, null,
   * String.valueOf(rawPassword));
   * 
   * if (LOG.isDebugEnabled()) {
   * LOG.debug(String.format("Password changed for user '%s' in zone '%s'.",
   * userName, syncZoneId)); } } else { // if not, then it's a regular internal
   * user and should be handled // accordingly super.updateUser(userName,
   * rawPassword);
   * 
   * if (LOG.isDebugEnabled()) {
   * LOG.debug(String.format("Password changed for user '%s'.", userName)); } }
   * }
   */
  public void setSyncZoneId(String syncZoneId) {
    this.syncZoneId = syncZoneId;
  }

  public void setLdapUserService(LdapUserService ldapUserService) {
    this.ldapUserService = ldapUserService;
  }

  @Override
  public void setAuthorityService(AuthorityService authorityService) {
    this.authorityService = authorityService;
    super.setAuthorityService(authorityService);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    super.afterPropertiesSet();

    Assert.hasText(syncZoneId);
    Assert.notNull(ldapUserService);
  }

}
