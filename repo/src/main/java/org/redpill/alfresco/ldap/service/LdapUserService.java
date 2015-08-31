package org.redpill.alfresco.ldap.service;

public interface LdapUserService {
  
  /**
   * Change user password
   * @param userId The user id
   * @param oldPassword the old password. If null, the password change operation will be run in the bind context.
   * @param newPassword the new password
   */
  public void changePassword(String userId, String oldPassword, String newPassword);
}
