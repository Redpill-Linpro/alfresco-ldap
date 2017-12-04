package org.redpill.alfresco.ldap.security.authentication.it;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
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

public class CustomRepositoryAuthenticationDaoIT extends AbstractLdapRepoIT {

  public String DEFAULT_USERNAME;
  public char[] DEFAULT_PASSWORD;

  private LdapUserService _ldapUserService;

  @Before
  public void setUp() {
    ApplicationContext ctx = getApplicationContext();
    _ldapUserService = (LdapUserService) ctx.getBean("rl.ldapUserService");
  }

  @After
  public void afterClassSetup() {
    authenticationComponent.clearCurrentSecurityContext();
  }

  @Ignore
  @Test
  public void testCreateAlfrescoPerson() {
    DEFAULT_USERNAME = "howland-" + GUID.generate();
    DEFAULT_PASSWORD = "superduper".toCharArray();
    transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>() {
      @Override
      public NodeRef execute() throws Throwable {
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME);

        authenticationService.createAuthentication(DEFAULT_USERNAME, "password".toCharArray());

        PropertyMap properties = new PropertyMap(3);
        properties.put(ContentModel.PROP_USERNAME, DEFAULT_USERNAME);
        properties.put(ContentModel.PROP_FIRSTNAME, "Howland");
        properties.put(ContentModel.PROP_LASTNAME, "Simpson");
        properties.put(ContentModel.PROP_EMAIL, "testmail@.malinator.com");

        personService.createPerson(properties);
        return null;
      }
    }, false, isRequiresNew());

    try {
      transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>() {
        @Override
        public NodeRef execute() throws Throwable {
          AuthenticationUtil.setFullyAuthenticatedUser(DEFAULT_USERNAME);

          // first change the password for the DEFAULT_USERNAME
          authenticationService.updateAuthentication(DEFAULT_USERNAME, "password".toCharArray(),
                  DEFAULT_PASSWORD);

          // try to authenticate
          authenticationComponent.authenticate(DEFAULT_USERNAME, DEFAULT_PASSWORD);

          AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());

          // set the admin password
          authenticationService.setAuthentication(AuthenticationUtil.getAdminUserName(),
                  "verysecure".toCharArray());

          // then authenticate
          authenticationComponent.authenticate(AuthenticationUtil.getAdminUserName(),
                  "verysecure".toCharArray());

          // change back the admin password
          authenticationService.setAuthentication(AuthenticationUtil.getAdminUserName(),
                  "admin".toCharArray());

          return null;
        }
      }, false, isRequiresNew());
    } finally {
      transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>() {
        @Override
        public NodeRef execute() throws Throwable {
          AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.SYSTEM_USER_NAME);
          personService.deletePerson(DEFAULT_USERNAME);
          _ldapUserService.deleteUser(DEFAULT_USERNAME);
          return null;
        }
      }, false, isRequiresNew());
    }
  }

}
