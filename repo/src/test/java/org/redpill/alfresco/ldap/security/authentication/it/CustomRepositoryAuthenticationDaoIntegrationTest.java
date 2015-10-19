package org.redpill.alfresco.ldap.security.authentication.it;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;
import org.junit.Test;
import org.redpill.alfresco.ldap.it.AbstractLdapRepoIntegrationTest;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.springframework.beans.factory.annotation.Autowired;

public class CustomRepositoryAuthenticationDaoIntegrationTest extends AbstractLdapRepoIntegrationTest {

  public static final String DEFAULT_USERNAME = "howland-" + GUID.generate();
  public static final char[] DEFAULT_PASSWORD = "superduper".toCharArray();

  @Autowired
  private LdapUserService _ldapUserService;

  @Override
  public void afterClassSetup() {
    super.afterClassSetup();

    _authenticationComponent.clearCurrentSecurityContext();
  }

  @Test
  public void testCreateAlfrescoPerson() {
    AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME);

    _authenticationService.createAuthentication(DEFAULT_USERNAME, "password".toCharArray());

    PropertyMap properties = new PropertyMap(3);
    properties.put(ContentModel.PROP_USERNAME, DEFAULT_USERNAME);
    properties.put(ContentModel.PROP_FIRSTNAME, "Howland");
    properties.put(ContentModel.PROP_LASTNAME, "Simpson");
    properties.put(ContentModel.PROP_EMAIL, _properties.getProperty("mail.to.default"));

    _personService.createPerson(properties);

    try {
      AuthenticationUtil.setFullyAuthenticatedUser(DEFAULT_USERNAME);

      // first change the password for the DEFAULT_USERNAME
      _authenticationService.updateAuthentication(DEFAULT_USERNAME, "password".toCharArray(), DEFAULT_PASSWORD);

      // try to authenticate
      _authenticationComponent.authenticate(DEFAULT_USERNAME, DEFAULT_PASSWORD);

      AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());

      // set the admin password
      _authenticationService.setAuthentication(AuthenticationUtil.getAdminUserName(), "verysecure".toCharArray());

      // then authenticate
      _authenticationComponent.authenticate(AuthenticationUtil.getAdminUserName(), "verysecure".toCharArray());

      // change back the admin password
      _authenticationService.setAuthentication(AuthenticationUtil.getAdminUserName(), "admin".toCharArray());
    } finally {
      AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME);

      _personService.deletePerson(DEFAULT_USERNAME);
      _ldapUserService.deleteUser(DEFAULT_USERNAME);
    }
  }

}
