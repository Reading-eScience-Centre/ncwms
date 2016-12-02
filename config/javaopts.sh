#!/bin/sh

# Constants
JMX_PORT=9090
RMI_HOSTNAME="0.0.0.0"
MEMORY="4G"

# CLI args to include profiling via JMX
if [ "$#" -eq 1 ]; then
    if [ "$1" == "-p" ]; then
        profile=true
        PROFILE="-Dcom.sun.management.jmxremote.rmi.port=$JMX_PORT -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=$JMX_PORT -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=$RMI_HOSTNAME"
    fi
fi

# JAVA_OPTS
NORMAL="-server -d64 -Xms$MEMORY -Xmx$MEMORY"
HEAP_DUMP="-XX:+HeapDumpOnOutOfMemoryError"
HEADLESS="-Djava.awt.headless=true"

JAVA_PREFS_SYSTEM_ROOT="-Djava.util.prefs.systemRoot=$CATALINA_HOME/content/thredds/javaUtilPrefs -Djava.util.prefs.userRoot=$CATALINA_HOME/content/thredds/javaUtilPrefs"

JAVA_OPTS="$JAVA_OPTS $JAVA_PREFS_SYSTEM_ROOT $NORMAL $MAX_PERM_GEN $HEAP_DUMP $HEADLESS $PROFILE"
echo $JAVA_OPTS
