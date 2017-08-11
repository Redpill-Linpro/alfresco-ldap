package org.redpill.alfresco.ldap.scripts.person;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.WebScriptRequest;

import org.redpill.alfresco.ldap.scripts.person.CustomChangePasswordPost;

public class CustomChangePasswordPostTest {

  Mockery m;

  CustomChangePasswordPost ccpp;
  WebScriptRequest req;
  LdapUserService ldapUserService;
  AuthorityService authorityService;
  MutableAuthenticationService authenticationService;
  final static String SYNC_ZONE_ID = "customldap";
  final static String NEW_PASSWORD = "newpassword";
  final static String OLD_PASSWORD = "oldpassword";

  @Before
  public void setUp() {
    m = new JUnit4Mockery();

    authenticationService = m.mock(MutableAuthenticationService.class);
    authorityService = m.mock(AuthorityService.class);
    ldapUserService = m.mock(LdapUserService.class);
    req = m.mock(WebScriptRequest.class);

    ccpp = new CustomChangePasswordPost();
    ccpp.setLdapUserService(ldapUserService);
    ccpp.setAuthorityService(authorityService);
    ccpp.setAuthenticationService(authenticationService);
    ccpp.setSyncZoneId(SYNC_ZONE_ID);
    ccpp.setEnabled(true);

  }

  @After
  public void tearDown() {
    m.assertIsSatisfied();
  }

  @Test
  public void changeLdapPasswordAsAdminTest() throws IOException {
    final String USERNAME = "admin";
    final String JSON = "{\"newpw\":\"" + NEW_PASSWORD + "\"}";
    final Set<String> AUTHORITY_ZONES = new HashSet<String>(1);
    AUTHORITY_ZONES.add("AUTH.EXT." + SYNC_ZONE_ID);
    final Content content = m.mock(Content.class);
    m.checking(new Expectations() {
      {
        allowing(req).getExtensionPath();
        will(returnValue(USERNAME));
        allowing(content).getContent();
        will(returnValue(JSON));
        allowing(req).getContent();
        will(returnValue(content));
        allowing(authorityService).getAuthorityZones(USERNAME);
        will(returnValue(AUTHORITY_ZONES));
        oneOf(ldapUserService).changePassword(USERNAME, null, NEW_PASSWORD);
        allowing(authorityService).hasAdminAuthority();
        will(returnValue(true));
        allowing(authenticationService).getCurrentUserName();
        will(returnValue(" "));
      }
    });

    Map<String, Object> result = ccpp.executeImpl(req, null);
    assertNotNull(result);
    Boolean result2 = (Boolean) result.get("success");
    assertNotNull(result2);
    assertTrue(result2);
  }

  @Test
  public void changeLdapPasswordAsUserTest() throws IOException {
    final String USERNAME = "admin";
    final String JSON = "{\"newpw\":\"" + NEW_PASSWORD + "\",\"oldpw\":\"" + OLD_PASSWORD + "\"}";
    final Set<String> AUTHORITY_ZONES = new HashSet<String>(1);
    AUTHORITY_ZONES.add("AUTH.EXT." + SYNC_ZONE_ID);
    final Content content = m.mock(Content.class);
    m.checking(new Expectations() {
      {
        allowing(req).getExtensionPath();
        will(returnValue(USERNAME));
        allowing(content).getContent();
        will(returnValue(JSON));
        allowing(req).getContent();
        will(returnValue(content));
        allowing(authorityService).getAuthorityZones(USERNAME);
        will(returnValue(AUTHORITY_ZONES));
        oneOf(ldapUserService).changePassword(USERNAME, OLD_PASSWORD, NEW_PASSWORD);
        allowing(authorityService).hasAdminAuthority();
        will(returnValue(false));
        allowing(authenticationService).getCurrentUserName();
        will(returnValue(USERNAME));
      }
    });

    Map<String, Object> result = ccpp.executeImpl(req, null);
    assertNotNull(result);
    Boolean result2 = (Boolean) result.get("success");
    assertNotNull(result2);
    assertTrue(result2);
  }

  @Test
  public void changeLocalPasswordAsAdminTest() throws IOException {
    final String USERNAME = "admin";
    final String JSON = "{\"newpw\":\"" + NEW_PASSWORD + "\"}";
    final Set<String> AUTHORITY_ZONES = new HashSet<String>(1);
    // AUTHORITY_ZONES.add("AUTH.EXT." + SYNC_ZONE_ID);
    final Content content = m.mock(Content.class);
    m.checking(new Expectations() {
      {
        allowing(req).getExtensionPath();
        will(returnValue(USERNAME));
        allowing(content).getContent();
        will(returnValue(JSON));
        allowing(req).getContent();
        will(returnValue(content));
        allowing(authorityService).getAuthorityZones(USERNAME);
        will(returnValue(AUTHORITY_ZONES));
        // oneOf(ldapUserService).changePassword(USERNAME, null, NEW_PASSWORD);
        allowing(authorityService).hasAdminAuthority();
        will(returnValue(true));
        allowing(authenticationService).getCurrentUserName();
        will(returnValue(" "));
        allowing(authenticationService).setAuthentication(USERNAME, NEW_PASSWORD.toCharArray());
      }
    });

    Map<String, Object> result = ccpp.executeImpl(req, null);
    assertNotNull(result);
    Boolean result2 = (Boolean) result.get("success");
    assertNotNull(result2);
    assertTrue(result2);
  }

  @Test
  public void changeLocalPasswordAsUserTest() throws IOException {
    final String USERNAME = "admin";
    final String JSON = "{\"newpw\":\"" + NEW_PASSWORD + "\",\"oldpw\":\"" + OLD_PASSWORD + "\"}";
    final Set<String> AUTHORITY_ZONES = new HashSet<String>(1);
    // AUTHORITY_ZONES.add("AUTH.EXT." + SYNC_ZONE_ID);
    final Content content = m.mock(Content.class);
    m.checking(new Expectations() {
      {
        allowing(req).getExtensionPath();
        will(returnValue(USERNAME));
        allowing(content).getContent();
        will(returnValue(JSON));
        allowing(req).getContent();
        will(returnValue(content));
        allowing(authorityService).getAuthorityZones(USERNAME);
        will(returnValue(AUTHORITY_ZONES));
        // oneOf(ldapUserService).changePassword(USERNAME, null, NEW_PASSWORD);
        allowing(authorityService).hasAdminAuthority();
        will(returnValue(false));
        allowing(authenticationService).getCurrentUserName();
        will(returnValue(" "));
        allowing(authenticationService).updateAuthentication(USERNAME, OLD_PASSWORD.toCharArray(), NEW_PASSWORD.toCharArray());
      }
    });

    Map<String, Object> result = ccpp.executeImpl(req, null);
    assertNotNull(result);
    Boolean result2 = (Boolean) result.get("success");
    assertNotNull(result2);
    assertTrue(result2);
  }
}
