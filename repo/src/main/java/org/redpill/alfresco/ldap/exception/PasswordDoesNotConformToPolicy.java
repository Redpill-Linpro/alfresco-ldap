package org.redpill.alfresco.ldap.exception;

import org.alfresco.error.AlfrescoRuntimeException;

public class PasswordDoesNotConformToPolicy extends AlfrescoRuntimeException {

  public PasswordDoesNotConformToPolicy() {
    super("Password does not conform to policy");
  }
  
  public PasswordDoesNotConformToPolicy(Exception e) {
    super("Password does not conform to policy", e);
  }

  /**
   * 
   */
  private static final long serialVersionUID = 9026530937146623887L;
  
}
