package org.redpill.alfresco.ldap.service.impl.it;

import static org.junit.Assert.*;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
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
  
  @Test
  public void testUserChangePassword() {
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
    ldapUserService.changePassword("lisa" ,DEFAULT_PASSWORD, "springfield2");
    _authenticationService.authenticate("lisa", "springfield2".toCharArray());
    assertEquals("lisa", AuthenticationUtil.getFullyAuthenticatedUser());
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    ldapUserService.changePassword("lisa", "springfield2", DEFAULT_PASSWORD);
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
  }
  
  @Test
  public void testSystemChangePassword() {
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
    ldapUserService.changePassword("lisa" ,null, "springfield2");
    _authenticationService.authenticate("lisa", "springfield2".toCharArray());
    assertEquals("lisa", AuthenticationUtil.getFullyAuthenticatedUser());
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getSystemUserName());
    ldapUserService.changePassword("lisa", null, DEFAULT_PASSWORD);
    assertEquals(AuthenticationUtil.getSystemUserName(), AuthenticationUtil.getFullyAuthenticatedUser());
  }
}
