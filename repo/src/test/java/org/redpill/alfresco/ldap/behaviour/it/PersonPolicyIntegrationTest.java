package org.redpill.alfresco.ldap.behaviour.it;

import static org.junit.Assert.*;

import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.util.PropertyMap;
import org.junit.Test;
import org.redpill.alfresco.ldap.it.AbstractLdapRepoIntegrationTest;

public class PersonPolicyIntegrationTest extends AbstractLdapRepoIntegrationTest {

  public static final String USER_HOWLAND = "howland";

  @Override
  public void beforeClassSetup() {
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
  }

  @Override
  public void afterClassSetup() {
    super.afterClassSetup();
    _authenticationComponent.clearCurrentSecurityContext();
  }
  
  @Test
  public void testCreateAlfrescoPerson() {
    //NodeRef userNodeRef = createUser(USER_HOWLAND);
    PropertyMap properties = new PropertyMap(3);
    properties.put(ContentModel.PROP_USERNAME, USER_HOWLAND);
    properties.put(ContentModel.PROP_FIRSTNAME, "Howland");
    properties.put(ContentModel.PROP_LASTNAME, "Simpson");
    properties.put(ContentModel.PROP_EMAIL, _properties.getProperty("mail.to.default"));

    NodeRef userNodeRef = _personService.createPerson(properties);
    assertNotNull(userNodeRef);
    Set<String> authorityZones = _authorityService.getAuthorityZones(USER_HOWLAND);

    boolean inLdapAuthorityZone = false;
    for (String authorityZone : authorityZones) {
      if (authorityZone.startsWith(AuthorityService.ZONE_AUTH_EXT_PREFIX)) {
        inLdapAuthorityZone = true;
      }
    }
    
    assertTrue(inLdapAuthorityZone);
  }
}
