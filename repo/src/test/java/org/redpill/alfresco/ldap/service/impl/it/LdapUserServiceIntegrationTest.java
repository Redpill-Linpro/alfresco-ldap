package org.redpill.alfresco.ldap.service.impl.it;

import static org.junit.Assert.*;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.redpill.alfresco.ldap.it.AbstractLdapRepoIntegrationTest;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class LdapUserServiceIntegrationTest extends AbstractLdapRepoIntegrationTest {

  @Autowired
  @Qualifier("rl.ldapUserService")
  protected LdapUserService ldapUserService;

  private static final String DEFAULT_PASSWORD = "springfield";
  private static final String CHANGED_PASSWORD = "springfield2";
  private static final String USER_LISA = "lisa";
  private static final String USER_ABRAHAM = "abraham";
  private static final String USER_MONA = "mona";
  private static final String USER_TUT = "tut";
  private static final String USER_HAPPY = "happy";
  
  @Test
  public void testUserChangePassword() {
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
    ldapUserService.changePassword(USER_LISA, DEFAULT_PASSWORD, CHANGED_PASSWORD);
    _authenticationService.authenticate(USER_LISA, CHANGED_PASSWORD.toCharArray());
    assertEquals(USER_LISA, AuthenticationUtil.getFullyAuthenticatedUser());
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    ldapUserService.changePassword(USER_LISA, CHANGED_PASSWORD, DEFAULT_PASSWORD);
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
  }

  @Test
  public void testSystemChangePassword() {
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
    ldapUserService.changePassword(USER_LISA, null, CHANGED_PASSWORD);
    _authenticationService.authenticate(USER_LISA, CHANGED_PASSWORD.toCharArray());
    assertEquals(USER_LISA, AuthenticationUtil.getFullyAuthenticatedUser());
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    ldapUserService.changePassword(USER_LISA, null, DEFAULT_PASSWORD);
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
  }

  @Test
  public void testSystemAddUser() {
    _testSystemAddUser(USER_ABRAHAM);
  }

  protected void _testSystemAddUser(String user) {
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
    ldapUserService.createUser(user, DEFAULT_PASSWORD, user + "@simpson.com", StringUtils.capitalise(user), "Simpson");
    _authenticationService.authenticate(user, DEFAULT_PASSWORD.toCharArray());
    assertEquals(user, AuthenticationUtil.getFullyAuthenticatedUser());
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
  }

  @Test
  public void testSystemDeleteUser() {
    _testSystemAddUser(USER_MONA);
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
    ldapUserService.deleteUser(USER_MONA);
    try {
      _authenticationService.authenticate(USER_MONA, DEFAULT_PASSWORD.toCharArray());
      assertTrue(false);
    } catch (AuthenticationException e) {
      // Expected
    }
  }

  @Test
  public void testSystemEditUser() {
    _testSystemAddUser(USER_TUT);
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
    ldapUserService.editUser(USER_TUT, null, null, "tut2@simpson.com", "Tut2", "Simpson2");
  }
  
  @Test
  public void testUserEditUser() {
    _testSystemAddUser(USER_HAPPY);
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
    ldapUserService.editUser(USER_HAPPY, DEFAULT_PASSWORD, CHANGED_PASSWORD, "happy2@simpson.com", null, null);
  }

}
