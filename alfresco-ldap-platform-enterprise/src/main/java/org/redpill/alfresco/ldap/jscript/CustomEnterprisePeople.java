// @overridden projects/repository/source/java/org/alfresco/repo/jscript/People.java
package org.redpill.alfresco.ldap.jscript;

import java.util.Set;

import org.alfresco.enterprise.repo.jscript.EnterprisePeople;
import org.alfresco.repo.jscript.ScriptableHashMap;
import org.alfresco.service.cmr.security.AuthorityService;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;


/**
 * Override of Enterprise People script class.
 * @author Marcus Svartmark
 *
 */

public class CustomEnterprisePeople extends EnterprisePeople {
  private static final Logger LOG = Logger.getLogger(CustomEnterprisePeople.class);
  protected AuthorityService authorityService;
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

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(authorityService);
    Assert.hasText(syncZoneId);
    super.afterPropertiesSet();
  }
}
