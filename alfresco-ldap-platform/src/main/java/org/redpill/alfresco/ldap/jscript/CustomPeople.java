// @overridden projects/repository/source/java/org/alfresco/repo/jscript/People.java
package org.redpill.alfresco.ldap.jscript;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.People;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptableHashMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

public class CustomPeople extends People {

  private static final Logger LOG = Logger.getLogger(CustomPeople.class);
  protected AuthorityService authorityService;
  protected NodeService nodeService;
  protected String syncZoneId;
  protected boolean enabled;

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
  @SuppressWarnings("rawtypes")
  public ScriptableHashMap getImmutableProperties(String username) {
    // get all the authority zones for the user
    Set<String> authorityZones = authorityService.getAuthorityZones(username);

    if (enabled && authorityZones.contains("AUTH.EXT." + syncZoneId)) {
      LOG.debug(username + " is in zone " + syncZoneId + " bypassing immutable properties");
      return new ScriptableHashMap();
    } else {
      return super.getImmutableProperties(username);
    }
  }

  public Map<String, Boolean> getCapabilities(final ScriptNode person) {
    NodeRef nodeRef = person.getNodeRef();
    Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
    String userId = (String) properties.get(ContentModel.PROP_USERNAME);

    Map<String, Boolean> retVal = super.getCapabilities(person);

    Set<String> authorityZones = authorityService.getAuthorityZones(userId);
    if (enabled && authorityZones.contains("AUTH.EXT." + syncZoneId)) {
      LOG.debug(userId + " is in zone " + syncZoneId + ", set isMutable to true");
      retVal.replace("isMutable", true);
    }
    return retVal;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * <property name="honorHintUseCQ">
   * <value>${people.search.honor.hint.useCQ}</value> </property>
   *
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public void setHonorHintUseCQ2(String honorHintUseCQ) {
    Method[] methods = People.class.getMethods();
    for (Method method : methods) {
      if ("setHonorHintUseCQ".equals(method.getName())) {
        LOG.error("CALLING setHonorHintUseCQ");
        try {
          method.invoke(this, Boolean.parseBoolean(honorHintUseCQ));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          throw new AlfrescoRuntimeException("Error initializing hononHinUseCQ, value: " + honorHintUseCQ, e);
        }
      }
    }
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(authorityService);
    Assert.hasText(syncZoneId);
    super.afterPropertiesSet();
  }
}
