#!/bin/bash

#./set_password.sh <user> <password>

HASH=`php -r "echo sha1('$1');"`

$PGSQL_BIN/psql -v ON_ERROR_STOP=1 -X -c "UPDATE searchapp.search_auth_user SET \
		passwd = '$HASH' WHERE username = '$2'"
