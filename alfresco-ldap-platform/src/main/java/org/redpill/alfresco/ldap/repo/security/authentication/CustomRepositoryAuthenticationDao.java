/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redpill.alfresco.ldap.repo.security.authentication;

import java.util.HashSet;
import java.util.Set;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.RepositoryAuthenticationDao;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;

import net.sf.acegisecurity.providers.encoding.PasswordEncoder;
import org.alfresco.service.cmr.security.AuthorityService;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 *
 * @author Jimmie Aleksic
 */
public class CustomRepositoryAuthenticationDao extends RepositoryAuthenticationDao implements InitializingBean {

  private static final Logger logger = Logger.getLogger(CustomRepositoryAuthenticationDao.class);
  protected boolean enabled;
  protected PasswordEncoder passwordEncoder;
  protected LdapUserService ldapUserService;
  protected String syncZoneId;

  @Override
  public void createUser(String caseSensitiveUserName, char[] rawPassword) throws AuthenticationException {
    super.createUser(caseSensitiveUserName, null, rawPassword);

    NodeRef nodeRef = authorityService.getAuthorityNodeRef(caseSensitiveUserName);
    String finalEmail = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_EMAIL);
    String firstName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_FIRSTNAME);
    String lastName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_LASTNAME);
    ldapUserService.createUser(caseSensitiveUserName, new String(rawPassword), false, finalEmail, firstName, lastName);

    // Add user to zone
    final String zoneName = AuthorityService.ZONE_AUTH_EXT_PREFIX + syncZoneId;
    final Set<String> zones = new HashSet<String>();
    zones.add(zoneName);

    authorityService.getOrCreateZone(zoneName);
    Set<String> authoritiesForUser = authorityService.getAuthorityZones(caseSensitiveUserName);
    if (!authoritiesForUser.contains(zoneName)) {
      authorityService.addAuthorityToZones(caseSensitiveUserName, zones);
    }

    if (logger.isInfoEnabled()) {
      logger.info("Adding " + caseSensitiveUserName + " to zone " + zoneName);
    }
  }

  public void setLdapUserService(LdapUserService ldapUserService) {
    this.ldapUserService = ldapUserService;
  }

  public void setSyncZoneId(String syncZoneId) {
    this.syncZoneId = syncZoneId;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(ldapUserService);
    Assert.notNull(syncZoneId);
  }

}
