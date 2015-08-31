package org.redpill.alfresco.ldap.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
}
