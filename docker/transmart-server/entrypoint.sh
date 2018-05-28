#!/bin/sh

# set up wildfly admin password
$JBOSS_HOME/bin/add-user.sh admin $ADMIN_PASSWORD --silent

# run wildfly with transmart
exec /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0
