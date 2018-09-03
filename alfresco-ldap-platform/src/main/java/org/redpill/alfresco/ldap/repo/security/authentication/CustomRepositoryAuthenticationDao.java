/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redpill.alfresco.ldap.repo.security.authentication;

import net.sf.acegisecurity.providers.encoding.PasswordEncoder;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.RepositoryAuthenticationDao;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jimmie Aleksic
 */
public class CustomRepositoryAuthenticationDao extends RepositoryAuthenticationDao implements InitializingBean {

  private static final Logger LOG = Logger.getLogger(CustomRepositoryAuthenticationDao.class);
  protected boolean enabled;
  protected PasswordEncoder passwordEncoder;
  protected LdapUserService ldapUserService;
  protected String syncZoneId;
  protected PersonService personService;

  @Override
  public void createUser(String caseSensitiveUserName, char[] rawPassword) throws AuthenticationException {
    createUser(caseSensitiveUserName, null, rawPassword);
  }

  @Override
  public void createUser(String caseSensitiveUserName, String hashedPassword, char[] rawPassword) throws AuthenticationException {


    if (enabled) {
      NodeRef nodeRef = personService.getPersonOrNull(caseSensitiveUserName);
      if (nodeRef != null) {
        String finalEmail = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_EMAIL);
        String firstName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_FIRSTNAME);
        String lastName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_LASTNAME);
        LOG.trace("Creating user (user id, first name, last name, email) (" + caseSensitiveUserName + "," + firstName + "," + lastName + "," + finalEmail + ")");

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

        if (LOG.isInfoEnabled()) {
          LOG.info("Adding " + caseSensitiveUserName + " to zone " + zoneName);
        }
      } else {
        throw new AuthenticationException("Could not replicate user " + caseSensitiveUserName + " to ldap. User must be created with PersonService before its authentication is created.");
      }
    } else {
      super.createUser(caseSensitiveUserName, hashedPassword, rawPassword);
    }
  }

  @Override
  public void updateUser(String userName, char[] rawPassword) throws AuthenticationException {
    if (enabled) {
      // Remove user if in correct zone
      final String zoneName = AuthorityService.ZONE_AUTH_EXT_PREFIX + syncZoneId;
      Set<String> authoritiesForUser = authorityService.getAuthorityZones(userName);
      if (authoritiesForUser != null && authoritiesForUser.contains(zoneName)) {
        ldapUserService.changePassword(userName, null, new String(rawPassword));
      }
      else {
        super.updateUser(userName, rawPassword);
      }
    } else {
      super.updateUser(userName, rawPassword);
    }
  }

  @Override
  public void deleteUser(String username) {
    if (enabled) {
      // Remove user if in correct zone
      final String zoneName = AuthorityService.ZONE_AUTH_EXT_PREFIX + syncZoneId;
      Set<String> authoritiesForUser = authorityService.getAuthorityZones(username);
      if (authoritiesForUser != null && authoritiesForUser.contains(zoneName)) {
        ldapUserService.deleteUser(username);
      }
    }
    super.deleteUser(username);
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


  public void setPersonService(PersonService personService) {
    this.personService = personService;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(ldapUserService);
    Assert.notNull(syncZoneId);
    Assert.notNull(enabled);
    Assert.notNull(personService);
  }

}
