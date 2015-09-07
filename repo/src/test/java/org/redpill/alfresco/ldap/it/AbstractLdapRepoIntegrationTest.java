package org.redpill.alfresco.ldap.it;

import org.redpill.alfresco.test.AbstractRepoIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({ "classpath:alfresco/application-context.xml", "classpath:alfresco/test-spring-ldap-context.xml" })
public abstract class AbstractLdapRepoIntegrationTest extends AbstractRepoIntegrationTest {

}
