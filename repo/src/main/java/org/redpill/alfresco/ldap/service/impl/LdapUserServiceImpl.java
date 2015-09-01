package org.redpill.alfresco.ldap.service.impl;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
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
  private String[] objectClasses;
  private String userIdAttributeName;
  private String givenNameAttributeName;
  private String cnAttributeName;
  private String snAttributeName;
  private String mailAttributeName;
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

    final ModificationItem[] modItems = new ModificationItem[] { new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(passwordAttributeName, hashedPassword)) };

    executeUpdate(userId, oldPassword, modItems);
  }

  @Override
  public void createUser(final String userId, final String password, final String email, final String firstName, final String lastName) {

    logger.debug("Creating user " + userId);
    String hashedPassword = "";
    try {
      hashedPassword = LdapServiceUtils.hashMD5Password(password);
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e1) {
      logger.error(e1);
      throw new AlfrescoRuntimeException("Error hashing password", e1);
    }

    Attributes personAttributes = new BasicAttributes();
    BasicAttribute personBasicAttribute = new BasicAttribute("objectclass");
    for (String objectClass : objectClasses) {
      personBasicAttribute.add(objectClass);
    }
    personAttributes.put(personBasicAttribute);
    personAttributes.put(userIdAttributeName, userId);
    personAttributes.put(givenNameAttributeName, firstName);
    personAttributes.put(cnAttributeName, firstName);
    personAttributes.put(snAttributeName, lastName);

    personAttributes.put(passwordAttributeName, hashedPassword);
    personAttributes.put(mailAttributeName, email);

    final DistinguishedName dn = usernameMapper.buildDn(userId);

    ldapTemplate.bind(dn, null, personAttributes);

  }

  @Override
  public void editUser(final String userId, final String oldPassword, final String newPassword, final String email, final String firstName, final String lastName) {

    logger.debug("Editing user " + userId);

    List<ModificationItem> modItems = new ArrayList<ModificationItem>();

    if (newPassword != null) {
      String hashedPassword = newPassword;
      try {
        hashedPassword = LdapServiceUtils.hashMD5Password(newPassword);
        modItems.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(passwordAttributeName, hashedPassword)));
      } catch (NoSuchAlgorithmException | UnsupportedEncodingException e1) {
        logger.error(e1);
        throw new AlfrescoRuntimeException("Error hashing password", e1);
      }
    }

    if (email != null) {
      modItems.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(mailAttributeName, email)));
    }

    if (firstName != null) {
      modItems.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(givenNameAttributeName, firstName)));
      modItems.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(cnAttributeName, firstName)));
    }

    if (lastName != null) {
      modItems.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(snAttributeName, lastName)));
    }

    executeUpdate(userId, oldPassword, modItems.toArray(new ModificationItem[0]));

  }

  /**
   * Executes an update on the ldap directory
   * 
   * @param userId
   *          The user id to update
   * @param password
   *          The password (null if change as system user)
   * @param modItems
   *          The attributes to set
   */
  protected void executeUpdate(final String userId, final String password, final ModificationItem[] modItems) {
    final DistinguishedName dn = usernameMapper.buildDn(userId);
    if (password == null) {
      try {
        ldapTemplate.modifyAttributes(dn, modItems);
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
        ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
        try {
          ctx.reconnect(null);
        } catch (javax.naming.AuthenticationException e) {
          logger.error(e);
          throw new AuthenticationException("Authentication for password change failed.");
        }

        ctx.modifyAttributes(dn, modItems);

        return null;
      }
    });
  }

  @Override
  public void deleteUser(final String userId) {
    logger.debug("Deleting user " + userId);
    final DistinguishedName dn = usernameMapper.buildDn(userId);
    ldapTemplate.unbind(dn);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(contextSource, "You have to provide an instance of ContextSource");
    Assert.notNull(usernameMapper);
    Assert.notNull(passwordAttributeName);
    Assert.notNull(cnAttributeName);
    Assert.notNull(givenNameAttributeName);
    Assert.notNull(mailAttributeName);
    Assert.notNull(snAttributeName);
    Assert.notNull(userIdAttributeName);
    Assert.notNull(objectClasses);
    Assert.notEmpty(objectClasses);
    ldapTemplate = new LdapTemplate(contextSource);
    logger.info("Initalized " + this.getClass().getName());
  }

  public void setUsernameMapper(LdapUsernameToDnMapper usernameMapper) {
    this.usernameMapper = usernameMapper;
  }

  public void setContextSource(ContextSource contextSource) {
    this.contextSource = contextSource;
  }

  public void setPasswordAttributeName(String passwordAttributeName) {
    this.passwordAttributeName = passwordAttributeName;
  }

  public void setObjectClasses(String objectClasses) {
    String[] split = objectClasses.split(",");
    for (int i=0;i<split.length;i++) {
      split[i] = split[i].trim();
    }
    this.objectClasses = split;
  }

  public void setGivenNameAttributeName(String givenNameAttributeName) {
    this.givenNameAttributeName = givenNameAttributeName;
  }

  public void setSnAttributeName(String snAttributeName) {
    this.snAttributeName = snAttributeName;
  }

  public void setMailAttributeName(String mailAttributeName) {
    this.mailAttributeName = mailAttributeName;
  }
  
  public void setCnAttributeName(String cnAttributeName) {
    this.cnAttributeName = cnAttributeName;
  }
  
  public void setUserIdAttributeName(String userIdAttributeName) {
    this.userIdAttributeName = userIdAttributeName;
  }
  

}
