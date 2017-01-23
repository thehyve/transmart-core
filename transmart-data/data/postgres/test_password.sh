#!/bin/bash

#./set_password.sh <hash> <password>

SALT=`php -d open_basedir= gen_salt.php`

# jbcrypt only supports the 'a' revision
HASH=`php -r "echo crypt('$2', '$1');"`

echo "Hash  is: $HASH"
echo "Hash was: $1"


