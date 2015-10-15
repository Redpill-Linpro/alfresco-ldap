package org.redpill.alfresco.ldap.security.authentication;

import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.RepositoryAuthenticationDao;
import org.alfresco.service.cmr.security.AuthorityService;
import org.apache.log4j.Logger;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.springframework.util.Assert;

public class CustomRepositoryAuthenticationDao extends RepositoryAuthenticationDao {

  private static final Logger LOG = Logger.getLogger(CustomRepositoryAuthenticationDao.class);

  protected String syncZoneId;

  protected LdapUserService ldapUserService;
  protected AuthorityService authorityService;

  @Override
  public void updateUser(String userName, char[] rawPassword) throws AuthenticationException {
    // get all the authority zones for the user
    Set<String> authorityZones = authorityService.getAuthorityZones(userName);

    if (authorityZones.contains("AUTH.EXT." + syncZoneId)) {
      // if the user is in the configured ${ldapMgr.syncZoneId} then it's an
      // LDAP user
      ldapUserService.changePassword(userName, null, String.valueOf(rawPassword));

      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("Password changed for user '%s' in zone '%s'.", userName, syncZoneId));
      }
    } else {
      // if not, then it's a regular internal user and should be handled
      // accordingly
      super.updateUser(userName, rawPassword);

      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("Password changed for user '%s'.", userName));
      }
    }
  }

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
