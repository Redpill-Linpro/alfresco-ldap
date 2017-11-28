#!/bin/bash
if [[ -z ${MAVEN_OPTS} ]]; then
    echo "The environment variable 'MAVEN_OPTS' is not set, setting it for you";
    MAVEN_OPTS="-Xms256m -Xmx2G"
fi
# Read hotswap settings from home folder. Create a file called hotswap.sh in your home dir with the following contents:
# export MAVEN_OPTS="$MAVEN_OPTS -XXaltjvm=dcevm -javaagent:/home/<homedir>/hotswap-agent.jar"
. ~/hotswap.sh
echo "MAVEN_OPTS is set to '$MAVEN_OPTS'";
mvnDebug clean install alfresco:run
#mvnDebug clean install alfresco:run -Dmaven.test.skip=true
