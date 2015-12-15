package org.redpill.alfresco.ldap.model;

import org.alfresco.service.namespace.QName;

public interface RlLdapModel {

  public static final String RL_LDAP_URI = "http://www.redpill-linpro.com/model/ldap/1.0";
  public static final String RL_LDAP_SHORT = "rlldap";

  public static final QName ASPECT_TEMPORARY_PASSWORD = QName.createQName(RL_LDAP_URI, "temporaryPasswordAspect");

  public static final QName PROP_TEMPORARY_PASSWORD = QName.createQName(RL_LDAP_URI, "temporaryPassword");
  
  public static final QName ASPECT_PUSH_SYNC = QName.createQName(RL_LDAP_URI, "temporaryPushSyncAspect");

  public static final QName ASPECT_NO_PASSWORD = QName.createQName(RL_LDAP_URI, "noPasswordAspect");
}
