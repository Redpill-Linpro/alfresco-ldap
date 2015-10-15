/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.redpill.alfresco.ldap.scripts.person;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.web.scripts.person.ChangePasswordPost;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.util.Assert;

/**
 * Intercepts change password requests and redirects them to ldap service if applicable
 * @author Marcus Svartmark - Redpill Linpro AB
 *
 */
public class CustomChangePasswordPost extends ChangePasswordPost implements InitializingBean {
  private static final Logger LOG = Logger.getLogger(CustomChangePasswordPost.class);

  private static final String PARAM_NEWPW = "newpw";
  private static final String PARAM_OLDPW = "oldpw";
  protected String syncZoneId;
  protected boolean enabled;
  
  protected LdapUserService ldapUserService;
  protected AuthorityService authorityService;
  protected MutableAuthenticationService authenticationService;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.alfresco.repo.web.scripts.person.ChangePasswordPost#executeImpl(org.alfresco.web.scripts.WebScriptRequest, org.alfresco.web.scripts.Status)
   */
  @Override
  protected Map<String, Object> executeImpl(WebScriptRequest req, Status status) {
    // Extract user name from the URL - cannot be null or webscript desc would
    // not match
    String userName = req.getExtensionPath();

    // Extract old and new password details from JSON POST
    Content c = req.getContent();
    if (c == null) {
      throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Missing POST body.");
    }
    JSONObject json;
    try {
      json = new JSONObject(c.getContent());

      String oldPassword = null;
      String newPassword;

      // admin users can change/set a password without knowing the old one
      boolean isAdmin = authorityService.hasAdminAuthority();
      if (!isAdmin || (userName.equalsIgnoreCase(authenticationService.getCurrentUserName()))) {
        if (!json.has(PARAM_OLDPW) || json.getString(PARAM_OLDPW).length() == 0) {
          throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Old password 'oldpw' is a required POST parameter.");
        }
        oldPassword = json.getString(PARAM_OLDPW);
      }
      if (!json.has(PARAM_NEWPW) || json.getString(PARAM_NEWPW).length() == 0) {
        throw new WebScriptException(Status.STATUS_BAD_REQUEST, "New password 'newpw' is a required POST parameter.");
      }
      newPassword = json.getString(PARAM_NEWPW);

      // get all the authority zones for the user
      Set<String> authorityZones = authorityService.getAuthorityZones(userName);

      if (enabled && authorityZones.contains("AUTH.EXT." + syncZoneId)) {
        // if the user is in the configured ${ldapMgr.syncZoneId} then it's an
        // LDAP user
        ldapUserService.changePassword(userName, oldPassword, newPassword);

        if (LOG.isDebugEnabled()) {
          LOG.debug(String.format("Password changed for user '%s' in zone '%s'.", userName, syncZoneId));
        }
      } else {
        // if not, then it's a regular internal user and should be handled
        // accordingly
        if (LOG.isDebugEnabled()) {
          LOG.debug(String.format("'%s' is not an ldap user. Forwarding request to " + ChangePasswordPost.class.getName(), userName));
        }
        super.executeImpl(req, status);

      }

      // update the password
      // an Admin user can update without knowing the original pass - but must
      // know their own!
      if (!isAdmin || (userName.equalsIgnoreCase(authenticationService.getCurrentUserName()))) {
        authenticationService.updateAuthentication(userName, oldPassword.toCharArray(), newPassword.toCharArray());
      } else {
        authenticationService.setAuthentication(userName, newPassword.toCharArray());
      }
    } catch (AuthenticationException err) {
      throw new WebScriptException(Status.STATUS_UNAUTHORIZED, "Do not have appropriate auth or wrong auth details provided.");
    } catch (JSONException jErr) {
      throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Unable to parse JSON POST body: " + jErr.getMessage());
    } catch (IOException ioErr) {
      throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Unable to retrieve POST body: " + ioErr.getMessage());
    }

    Map<String, Object> model = new HashMap<String, Object>(1, 1.0f);
    model.put("success", Boolean.TRUE);
    return model;
  }

  public void setSyncZoneId(String syncZoneId) {
    this.syncZoneId = syncZoneId;
  }

  public void setLdapUserService(LdapUserService ldapUserService) {
    this.ldapUserService = ldapUserService;
  }
  
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setAuthorityService(AuthorityService authorityService) {
    this.authorityService = authorityService;
    super.setAuthorityService(authorityService);
  }
  
  @Override
  public void setAuthenticationService(MutableAuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
    super.setAuthenticationService(authenticationService);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.hasText(syncZoneId);
    Assert.notNull(ldapUserService);
    Assert.notNull(authorityService);
    Assert.notNull(authenticationService);
  }

}
