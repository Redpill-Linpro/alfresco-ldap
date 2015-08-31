package org.redpill.alfresco.ldap.service.impl;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.apache.log4j.Logger;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.redpill.alfresco.ldap.util.LdapServiceUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.ldap.LdapUsernameToDnMapper;
import org.springframework.security.ldap.LdapUtils;
import org.springframework.util.Assert;

public class LdapUserServiceImpl implements LdapUserService, InitializingBean {
  private static final Logger logger = Logger.getLogger(LdapUserServiceImpl.class);
  private LdapTemplate ldapTemplate;
  private ContextSource contextSource;
  private String passwordAttributeName;
  private LdapUsernameToDnMapper usernameMapper;

  @Override
  public void changePassword(final String userId, final String oldPassword, final String newPassword) {

    logger.debug("Changing password for user " + userId);
    String hashedPassword = newPassword;
    try {
      hashedPassword = LdapServiceUtils.hashMD5Password(newPassword);
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e1) {
      logger.error(e1);
      throw new AlfrescoRuntimeException("Error hashing password", e1);
    }
    final DistinguishedName dn = usernameMapper.buildDn(userId);
    final ModificationItem[] passwordChange = new ModificationItem[] { new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(passwordAttributeName, hashedPassword)) };

    if (oldPassword == null) {
      try {
        ldapTemplate.modifyAttributes(dn, passwordChange);
      } catch (Exception e) {
        logger.error(e);
        throw e;
      }
      return;
    }

    ldapTemplate.executeReadWrite(new ContextExecutor() {

      public Object executeWithContext(DirContext dirCtx) throws NamingException {

        LdapContext ctx = (LdapContext) dirCtx;
        ctx.removeFromEnvironment("com.sun.jndi.ldap.connect.pool");
        String fullDn = LdapUtils.getFullDn(dn, ctx).toString();
        logger.trace("Trying to connect with DN: " + fullDn);
        // logger.trace("Trying to connect password: " + oldPassword);
        ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, fullDn);
        ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, oldPassword);
        try {
          ctx.reconnect(null);
        } catch (javax.naming.AuthenticationException e) {
          logger.error(e);
          throw new AuthenticationException("Authentication for password change failed.");
        }

        ctx.modifyAttributes(dn, passwordChange);

        return null;
      }
    });
  }

  @Override
  public void createUser(String userId, String password, String email, String firstName, String lastName) {
    throw new UnsupportedOperationException("Not yet implemented!");
  }

  @Override
  public void deleteUser(String userId) {
    throw new UnsupportedOperationException("Not yet implemented!");
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(contextSource, "You have to provide an instance of ContextSource");
    Assert.notNull(usernameMapper);
    Assert.notNull(passwordAttributeName);
    ldapTemplate = new LdapTemplate(contextSource);
    logger.info("Initalized" + this.getClass().getName());
  }

  public void setUsernameMapper(LdapUsernameToDnMapper usernameMapper) {
    this.usernameMapper = usernameMapper;
  }

  public void setContextSource(ContextSource contextSource) {
    this.contextSource = contextSource;
  }

  public void setLdapTemplate(LdapTemplate ldapTemplate) {
    this.ldapTemplate = ldapTemplate;
  }

  public void setPasswordAttributeName(String passwordAttributeName) {
    this.passwordAttributeName = passwordAttributeName;
  }

}
