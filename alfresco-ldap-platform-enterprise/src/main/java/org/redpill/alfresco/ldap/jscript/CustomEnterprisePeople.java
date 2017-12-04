// @overridden projects/repository/source/java/org/alfresco/repo/jscript/People.java
package org.redpill.alfresco.ldap.jscript;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.alfresco.enterprise.repo.jscript.EnterprisePeople;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptableHashMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.alfresco.service.namespace.QName;

/**
 * Override of Enterprise People script class.
 *
 * @author Marcus Svartmark
 * @author Jimmie Aleksic
 *
 */
public class CustomEnterprisePeople extends EnterprisePeople {

  private static final Logger LOG = Logger.getLogger(CustomEnterprisePeople.class);
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
