package org.redpill.alfresco.ldap.service;

/**
 * Ldap user service - interface for service which manages users in a ldap
 * catalogue
 * 
 * @author Marcus Svartmark - Redpill Linpro AB
 *
 */
public interface LdapUserService {

  /**
   * Change user password
   * 
   * @param userId
   *          The user id
   * @param oldPassword
   *          the old password. If null, the password change operation will be
   *          run in the bind context.
   * @param newPassword
   *          the new password
   */
  public void changePassword(String userId, String oldPassword, String newPassword);

  /**
   * Create a user
   * 
   * @param userId
   *          The user id
   * @param password
   *          The password
   * @param email
   *          The email
   * @param firstName
   *          The first name
   * @param lastName
   *          The last name
   */
  public void createUser(String userId, String password, String email, String firstName, String lastName);

  /**
   * Create a user
   * 
   * @param userId
   *          The user id
   * @param password
   *          The password
   * @param doNotHash
   *          Toggle hashing of password. If not used, the client is responsible
   *          for hashing (or not) and putting a password in the correct format
   *          in the variable
   * @param email
   *          The email
   * @param firstName
   *          The first name
   * @param lastName
   *          The last name
   */
  public void createUser(String userId, String password, boolean doNotHash, String email, String firstName, String lastName);

  /**
   * Modify a user
   * 
   * @param userId
   *          The user id (not modifyable)
   * @param oldpassword
   *          The old password (null if system change, otherwise authenticate as
   *          user)
   * @param newPassword
   *          The new password (null if not change)
   * @param email
   *          The email (null if not change)
   * @param firstName
   *          The first name (null if not change)
   * @param lastName
   *          The last name (null if not change)
   */
  public void editUser(String userId, String oldpassword, String newPassword, String email, String firstName, String lastName);

  /**
   * Delete a user
   * 
   * @param userId
   *          The user id to delete
   */
  public void deleteUser(String userId);

}
