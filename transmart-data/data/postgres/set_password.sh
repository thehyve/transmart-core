#!/bin/bash

#./set_password.sh <user> <password>

SALT=`php -d open_basedir= gen_salt.php`

# jbcrypt only supports the 'a' revision
HASH=`php -r "echo crypt('$2', '\\$2a\\$14\\$$SALT');"`

echo "Hash is: $HASH (salt $SALT)"

$PGSQL_BIN/psql -v ON_ERROR_STOP=1 -X -c "UPDATE searchapp.search_auth_user SET \
		passwd = '$HASH' WHERE username = '$1'"
