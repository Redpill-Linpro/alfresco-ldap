package org.redpill.alfresco.ldap.behaviour.it;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.util.PropertyMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.ldap.it.AbstractLdapRepoIT;

import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PersonPolicyIT extends AbstractLdapRepoIT {

  @Before
  public void setUp() {
    authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
  }

  @After
  public void afterClassSetup() {
    authenticationComponent.clearCurrentSecurityContext();
  }


  @Test
  public void test() {
    
  }
}
