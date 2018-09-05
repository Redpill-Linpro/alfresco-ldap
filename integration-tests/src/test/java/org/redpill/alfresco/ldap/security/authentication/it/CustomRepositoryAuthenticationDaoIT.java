package org.redpill.alfresco.ldap.security.authentication.it;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.redpill.alfresco.ldap.it.AbstractLdapRepoIT;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.*;

public class CustomRepositoryAuthenticationDaoIT extends AbstractLdapRepoIT {

  public String DEFAULT_USERNAME;
  public char[] DEFAULT_PASSWORD;

  private LdapUserService _ldapUserService;

  private MutableAuthenticationDao authenticationDao;

  @Before
  public void setUp() {
    ApplicationContext ctx = getApplicationContext();
    _ldapUserService = (LdapUserService) ctx.getBean("rl.ldapUserService");

    authenticationDao = (MutableAuthenticationDao) ctx.getBean("authenticationDao");
  }

  @After
  public void afterClassSetup() {
    authenticationComponent.clearCurrentSecurityContext();
  }


  @Test
  public void testCRUD() {

    //CREATE
    AuthenticationUtil.clearCurrentSecurityContext();
    AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();

    DEFAULT_USERNAME = "howland-" + GUID.generate();
    DEFAULT_PASSWORD = "superduper".toCharArray();


    PropertyMap properties = new PropertyMap(4);
    properties.put(ContentModel.PROP_USERNAME, DEFAULT_USERNAME);
    properties.put(ContentModel.PROP_FIRSTNAME, "Howland");
    properties.put(ContentModel.PROP_LASTNAME, "Simpson");
    properties.put(ContentModel.PROP_EMAIL, "testmail@.malinator.com");

    personService.createPerson(properties);
    authenticationService.createAuthentication(DEFAULT_USERNAME, "password".toCharArray());

    try {
      authenticationComponent.authenticate(DEFAULT_USERNAME, DEFAULT_PASSWORD);
      assertFalse("Authentication should not succeed", true);
    } catch (AuthenticationException e) {
      //Expected
    }

    try {
      authenticationComponent.authenticate(DEFAULT_USERNAME, "password".toCharArray());
    } catch (AuthenticationException e) {
      //Expected
    }


    //EDIT
    AuthenticationUtil.clearCurrentSecurityContext();
    AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();

    authenticationService.setAuthentication(DEFAULT_USERNAME, DEFAULT_PASSWORD);

    try {
      authenticationComponent.authenticate(DEFAULT_USERNAME, DEFAULT_PASSWORD);
    } catch (AuthenticationException e) {
      throw e;
    }

    // authenticationService.updateAuthentication(); does not work with the ldap module

    //DELETE
    AuthenticationUtil.clearCurrentSecurityContext();
    AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();


    assertNotNull("User should exist", personService.getPersonOrNull(DEFAULT_USERNAME));


    personService.deletePerson(DEFAULT_USERNAME);

    assertNull("User should no longer exist", personService.getPersonOrNull(DEFAULT_USERNAME));
  }

}
