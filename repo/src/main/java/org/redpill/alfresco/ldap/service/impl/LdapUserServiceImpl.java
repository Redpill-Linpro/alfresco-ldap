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
import org.redpill.alfresco.ldap.exception.PasswordDoesNotConformToPolicy;
import org.redpill.alfresco.ldap.service.LdapUserService;
import org.redpill.alfresco.ldap.util.LdapServiceUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.ldap.InvalidAttributeValueException;
import org.springframework.ldap.NameAlreadyBoundException;
import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.ldap.LdapUsernameToDnMapper;
import org.springframework.security.ldap.LdapUtils;
import org.springframework.util.Assert;

/**
 * Ldap user service - implementation for service which manages users in a ldap
 * catalogue.
 * 
 * Active Directory resource: http://wetfeetblog.com/spring-ldap-microsoft-ad/60
 * 
 * @author Marcus Svartmark - Redpill Linpro AB
 *
 */
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
  private String passwordAlgorithm;
  private String cnBasedOn;

  @Override
  public void changePassword(final String userId, final String oldPassword, final String newPassword) {

    logger.debug("Changing password for user " + userId);
    Object hashedPassword = newPassword;
    try {
      hashedPassword = generatePassword(newPassword);
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e1) {
      logger.error(e1);
      throw new AlfrescoRuntimeException("Error hashing password", e1);
    }

    final ModificationItem[] modItems = new ModificationItem[] { new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(passwordAttributeName, hashedPassword)) };
    try {
      executeUpdate(userId, oldPassword, modItems);
    } catch (InvalidAttributeValueException e) {
      if (e.getMessage().contains("LDAP: error code 19") && e.getMessage().contains("unicodePwd") && e.getMessage().contains("CONSTRAINT_ATT_TYPE")) {
        throw new PasswordDoesNotConformToPolicy(e);
      } else {
        throw e;
      }
    }
  }

  @Override
  public void createUser(final String userId, final String password, final String email, final String firstName, final String lastName) {
    createUser(userId, password, false, email, firstName, lastName);
  }

  @Override
  public void createUser(final String userId, final String password, boolean doNotHash, final String email, final String firstName, final String lastName) {

    logger.debug("Creating user " + userId);
    Object hashedPassword = password;
    if (!doNotHash) {

      try {
        hashedPassword = generatePassword(password);
      } catch (NoSuchAlgorithmException | UnsupportedEncodingException e1) {
        logger.error(e1);
        throw new AlfrescoRuntimeException("Error hashing password", e1);
      }
    }

    Attributes personAttributes = new BasicAttributes();
    BasicAttribute personBasicAttribute = new BasicAttribute("objectclass");
    for (String objectClass : objectClasses) {
      personBasicAttribute.add(objectClass);
    }
    personAttributes.put(personBasicAttribute);
    personAttributes.put(userIdAttributeName, userId);
    personAttributes.put(givenNameAttributeName, firstName);
    if ("givenName".equalsIgnoreCase(cnBasedOn)) {
      personAttributes.put(cnAttributeName, firstName);
    } else if ("uid".equalsIgnoreCase(cnBasedOn)) {
      personAttributes.put(cnAttributeName, userId);
    } else {
      throw new UnsupportedOperationException("Invalid mapping for CN");
    }
    personAttributes.put(snAttributeName, lastName);

    personAttributes.put(passwordAttributeName, hashedPassword);
    personAttributes.put(mailAttributeName, email);

    final DistinguishedName dn = usernameMapper.buildDn(userId);
    try {
      ldapTemplate.bind(dn, null, personAttributes);
    } catch (NameAlreadyBoundException e1) {
      logger.debug("User already exist in ldap, aborting creation.", e1);
    } 
    catch (Exception e) {
      if (e.getMessage().contains("LDAP: error code 19") && e.getMessage().contains("unicodePwd") && e.getMessage().contains("CONSTRAINT_ATT_TYPE")) {
        throw new PasswordDoesNotConformToPolicy(e);
      } else {
        throw e;
      }
    }
  }

  @Override
  public void editUser(final String userId, final String oldPassword, final String newPassword, final String email, final String firstName, final String lastName) {

    logger.debug("Editing user " + userId);

    List<ModificationItem> modItems = new ArrayList<ModificationItem>();

    if (newPassword != null) {
      Object hashedPassword = newPassword;
      try {
        if (oldPassword == null) {
          hashedPassword = generatePassword(newPassword);
          modItems.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(passwordAttributeName, hashedPassword)));
        } else {
          //Active Directory require a remove/add if a user wants to change a password
          Object oldPassword2 = generatePassword(oldPassword);
          Object newPassword2 = generatePassword(newPassword);
          modItems.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(passwordAttributeName, oldPassword2)));
          modItems.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(passwordAttributeName, newPassword2)));
        }
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

  /**
   * Convenience method to generate password. Algorithm is configured in
   * alfresco-global.properties
   * 
   * @param password
   *          The password to encode
   * @return An encoded password
   * @throws NoSuchAlgorithmException
   *           Is thrown if an unsupported encryption algorithm is detected
   * @throws UnsupportedEncodingException
   *           Is thrown if the password is encoded in something other than UTF8
   */
  protected Object generatePassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    if ("ssha".equalsIgnoreCase(passwordAlgorithm)) {
      return LdapServiceUtils.hashSSHAPassword(password);
    } else if ("sha".equalsIgnoreCase(passwordAlgorithm)) {
      return LdapServiceUtils.hashSHAPassword(password);
    } else if ("md5".equalsIgnoreCase(passwordAlgorithm)) {
      return LdapServiceUtils.hashMD5Password(password);
    } else if ("ad".equalsIgnoreCase(passwordAlgorithm)) {
      return LdapServiceUtils.hashADPassword(password);
    } else {
      throw new NoSuchAlgorithmException("Unsupported algorithm " + passwordAlgorithm);
    }
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
    Assert.notNull(passwordAlgorithm);
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
    for (int i = 0; i < split.length; i++) {
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

  public void setPasswordAlgorithm(String passwordAlgorithm) {
    this.passwordAlgorithm = passwordAlgorithm;
  }

  public void setCnBasedOn(String cnBasedOn) {
    this.cnBasedOn = cnBasedOn;
  }

}
