// @overridden projects/repository/source/java/org/alfresco/repo/jscript/People.java
package org.redpill.alfresco.ldap.jscript;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.jscript.People;
import org.alfresco.repo.jscript.ScriptableHashMap;
import org.alfresco.service.cmr.security.AuthorityService;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

public class CustomPeople extends People {
  private static final Logger LOG = Logger.getLogger(CustomPeople.class);
  protected AuthorityService authorityService;
  protected String syncZoneId;

  @Override
  public void setAuthorityService(AuthorityService authorityService) {
    this.authorityService = authorityService;
    super.setAuthorityService(authorityService);
  }

  public void setSyncZoneId(String syncZoneId) {
    this.syncZoneId = syncZoneId;
  }

  /**
   * Bypasses immutable properties if use exists in a specific zone
   * 
   * @see org.alfresco.repo.jscript.People#getImmutableProperties(String)
   */
  public ScriptableHashMap getImmutableProperties(String username) {
    // get all the authority zones for the user
    Set<String> authorityZones = authorityService.getAuthorityZones(username);

    if (authorityZones.contains("AUTH.EXT." + syncZoneId)) {
      LOG.debug(username + " is in zone " + syncZoneId + " bypassing immutable properties");
      return new ScriptableHashMap();
    } else {
      return super.getImmutableProperties(username);
    }
  }

  /**
   * <property name="honorHintUseCQ">
   * <value>${people.search.honor.hint.useCQ}</value> </property>
   * 
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */

  public void setHonorHintUseCQ2(boolean honorHintUseCQ) {
    Method[] methods = People.class.getMethods();
    for (Method method : methods) {
      if ("setHonorHintUseCQ".equals(method.getName())) {
        LOG.error("CALLING setHonorHintUseCQ");
        try {
          method.invoke(this, honorHintUseCQ);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          throw new AlfrescoRuntimeException("Error initializing hononHinUseCQ, value: "+honorHintUseCQ, e);
        }
      }
    }
  }

  public void setHonorHintUseCQ2(String honorHintUseCQ) {
    Method[] methods = People.class.getMethods();
    for (Method method : methods) {
      if ("setHonorHintUseCQ".equals(method.getName())) {
        LOG.error("CALLING setHonorHintUseCQ");
        try {
          method.invoke(this, Boolean.parseBoolean(honorHintUseCQ));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          throw new AlfrescoRuntimeException("Error initializing hononHinUseCQ, value: "+honorHintUseCQ, e);
        }
      }
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(authorityService);
    Assert.hasText(syncZoneId);
    super.afterPropertiesSet();
  }
}
