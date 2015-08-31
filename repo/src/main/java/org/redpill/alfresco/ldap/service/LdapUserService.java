package org.redpill.alfresco.ldap.service;

/**
 * Ldap user service - interface for service which manages users in a ldap catalogue
 * @author Marcus Svartmark - Redpill Linpro AB
 *
 */
public interface LdapUserService {
  
  /**
   * Change user password
   * @param userId The user id
   * @param oldPassword the old password. If null, the password change operation will be run in the bind context.
   * @param newPassword the new password
   */
  public void changePassword(String userId, String oldPassword, String newPassword);
  
  /**
   * Create a user
   * @param userId The user id
   * @param password The password
   * @param email The email
   * @param firstName The first name
   * @param lastName The last name
   */
  public void createUser(String userId, String password, String email, String firstName, String lastName);
  
  /**
   * Delete a user
   * @param userId The user id to delete
   */
  public void deleteUser(String userId);
}