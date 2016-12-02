#!/bin/bash
set -e

if [ "$1" = 'catalina.sh' ]; then
    chown -R tomcat:tomcat .
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
