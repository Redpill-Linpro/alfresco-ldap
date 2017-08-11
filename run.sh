#!/bin/bash
if [[ -z ${MAVEN_OPTS} ]]; then
    echo "The environment variable 'MAVEN_OPTS' is not set, setting it for you";
    MAVEN_OPTS="-Xms256m -Xmx2G"
fi
export MAVEN_OPTS="-Xms1G -Xmx4G -Dmaven.junit.usefile=false -XX:-MaxFDLimit  -DfailIfNoTests=false"
export MAVEN_OPTS="$MAVEN_OPTS -XXaltjvm=dcevm -javaagent:/home/jimmie/Documents/alfresco/hotswap-agent-1.0.jar"
echo "MAVEN_OPTS is set to '$MAVEN_OPTS'";
mvn clean install alfresco:run
