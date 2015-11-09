# Alfresco Ldap Manager

This module allows for managing alfresco internal users in an ldap directory.

## Features
* External users invited to Alfresco are placed in an ldap
* Existing alfresco internal (alfrescoNtlm-users) can be moved to an ldap
* Passwords are stored in ldap
* Login is done against the ldap directory in the authentication chain

## Building & packaging
This module is distributed as jar files. They can be built and installed into your local maven repository using "mvn clean install".

## Installation
The files are also available in the Redpill Linpro public nexus server, which can be accessed in your own maven projects by adding the following dependencies and repository tags to your maven pom.xml for you repository project:

    <dependencies>
      <dependency>
        <groupId>org.redpill-linpro.alfresco.ldap</groupId>
        <artifactId>alfresco-ldap-repo</artifactId>
        <version>1.0.0</version>
      </dependency>
    </dependencies>


    <repositories>
      <repository>
        <id>redpill-linpro</id>
        <url>https://maven.redpill-linpro.com/nexus/content/groups/public</url>
        <snapshots>
          <enabled>true</enabled>
          <updatePolicy>daily</updatePolicy>
        </snapshots>
      </repository>
    </repositories>

## Configuration
Below are the default configuration parameters for the module. These can be modified to fit your own environment.

    ldapMgr.java.naming.provider.url=ldap://localhost:33389
    ldapMgr.java.naming.security.principal=uid=admin,ou=system
    ldapMgr.java.naming.security.credentials=secret
    ldapMgr.personQuery=(objectclass=organizationalPerson)
    ldapMgr.userSearchBase=ou=users,dc=test,dc=alfresco,dc=redpill,dc=org
    ldapMgr.groupSearchBase=ou=groups,dc=test,dc=alfresco,dc=redpill,dc=org
    
    ldapMgr.objectClasses=person,organizationalPerson,inetOrgPerson
    ldapMgr.passwordAttributeName=userPassword
    # Uncomment the line below for AD
    #ldapMgr.passwordAttributeName=unicodepwd
    ldapMgr.givenNameAttributeName=givenName
    ldapMgr.cnAttributeName=cn
    #ldapMgr.cnBasedOn=givenName
    ldapMgr.cnBasedOn=uid
    ldapMgr.snAttributeName=sn
    ldapMgr.mailAttributeName=mail
    ldapMgr.userIdAttributeName=uid
    
    ldapMgr.syncZoneId=ldap1
    #Can be either ssha, sha, md5 or ad. Default is ssha
    ldapMgr.passwordAlgorithm=ssha
    
    #Set to true to remove password when users are migrated to the ldap directory
    ldapMgr.resetPasswordOnPushSync=false
    
    ldapMgr.enabled=false

You also need to add your ldap directory to your autentication chain and add synchronization as described in the Alfresco Documentation.

## Migration

To migrate existing users to your ldap you can use the following code snippet in the JavaScript Console:


    var node = search.findNode("workspace://SpacesStore/AUTH.ALF");
    
    
    var num = 0;
    for each(p in node.children) {
    	if (p.typeShort != 'cm:person') {
    		continue;
    	}
    	if (p.hasAspect("rlldap:temporaryPushSyncAspect")) {
    		continue;
    	}
    	if (p.properties.userName.toLowerCase().equals("admin")) {
    		continue;	
    	}
    	if (p.properties.userName.toLowerCase().equals("system")) {
    		continue;	
    	}
    	if (p.properties.userName.toLowerCase().equals("systemuser")) {
    		continue;	
    	}
    	if (p.properties.userName.toLowerCase().equals("guest")) {
    		continue;	
    	}
    
    	try {
    	  p.addAspect("rlldap:temporaryPushSyncAspect");
    	} finally {
          //Do we really want to remove this aspect now? It could be used to send out new passwords etc.
    	  //p.removeAspect("rlldap:temporaryPushSyncAspect");
    		
    	}
    	
    	logger.log("Moved user to external AD: "+p.properties.userName);
    	num ++;
    	if (num >= 50) {
    		break;
    	}
    }
    logger.log("Number of users moved to ldap: "+num);

