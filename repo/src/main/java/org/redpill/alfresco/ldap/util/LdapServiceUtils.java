package org.redpill.alfresco.ldap.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.springframework.security.authentication.encoding.LdapShaPasswordEncoder;
import org.springframework.security.crypto.codec.Base64;

/**
 * Common ldap utils
 * @author Marcus Svartmark - Redpill Linpro
 *
 */
public class LdapServiceUtils {
  
  /**
   * Hash a password with md5 for LDAP
   * 
   * @param newPassword
   *          The password to hash
   * @return a md5 hashed password
   */
  public static String hashMD5Password(final String newPassword) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    MessageDigest digest = MessageDigest.getInstance("MD5");
    digest.update(newPassword.getBytes("UTF8"));
    String md5Password = new String(Base64.encode(digest.digest()));
    return "{MD5}" + md5Password;
  }
  
  /**
   * Hash a password with sha for LDAP
   * 
   * @param newPassword
   *          The password to hash
   * @return a sha hashed password
   */
  public static String hashSHAPassword(final String newPassword) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    LdapShaPasswordEncoder shaEncoder = new LdapShaPasswordEncoder();
    return shaEncoder.encodePassword(newPassword, null);
  }
  
  /**
   * Hash a password with ssha for LDAP
   * 
   * @param newPassword
   *          The password to hash
   * @return a ssha hashed password
   */
  public static String hashSSHAPassword(final String newPassword) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    LdapShaPasswordEncoder shaEncoder = new LdapShaPasswordEncoder();
    SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
    byte[] salt = new byte[64];
    random.nextBytes(salt);
    return shaEncoder.encodePassword(newPassword, salt);
  }
}
