# Alfresco Ldap Manager

This module allows for managing alfresco internal users in an ldap directory.

## Features
* External users invited to Alfresco are placed in an ldap
* Existing alfresco internal (alfrescoNtlm-users) can be moved to an ldap
* Passwords are stored in ldap
* Login is done against the ldap directory in the authentication chain

## Building & packaging
This module is distributed as jar files. They can be built and installed into your local maven repository using "mvn clean install".

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

## Installation

## Configuration

## Migration
