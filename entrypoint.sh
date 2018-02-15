#!/bin/bash
set -e

# preferable to fire up Tomcat via start-tomcat.sh which will start Tomcat with
# security manager, but inheriting containers can also start Tomcat via
# catalina.sh

if [ "$1" = 'start-tomcat.sh' ] || [ "$1" = 'catalina.sh' ]; then

    USER_ID=${TOMCAT_USER_ID:-1000}
    GROUP_ID=${TOMCAT_GROUP_ID:-1000}

    ###
    # Tomcat user
    ###
    groupadd -r tomcat -g ${GROUP_ID} && \
    useradd -u ${USER_ID} -g tomcat -d ${CATALINA_HOME} -s /sbin/nologin \
        -c "Tomcat user" tomcat

    chown -R tomcat:tomcat ${CATALINA_HOME} && chmod 400 ${CATALINA_HOME}/conf/*
    sync

    if [ ! -z "$DEFAULT_PALETTE" ]; then
        sed -i "/<\/Context>/i <Parameter name=\"defaultPalette\" value=\"$DEFAULT_PALETTE\" override=\"true\"/>" $CATALINA_HOME/conf/Catalina/localhost/ncWMS.xml
    fi

    if [ ! -z "$ADVERTISED_PALETTES" ]; then
        sed -i "/<\/Context>/i <Parameter name=\"advertisedPalettes\" value=\"$ADVERTISED_PALETTES\" override=\"true\"/>" $CATALINA_HOME/conf/Catalina/localhost/ncWMS.xml
    fi

    exec gosu tomcat "$@"
fi

exec "$@"
