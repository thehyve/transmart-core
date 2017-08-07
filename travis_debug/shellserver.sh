#!/bin/sh

LOCAL_USER=jan
LOCAL_ADDRESS=office.thehyve.net
LOCAL_PORT=34122
FORWARD_PORT1=34567
FORWARD_PORT2=5005

ls debug
chmod go-rwx travis_debug/id_rsa_test
ssh -N  -o StrictHostKeyChecking=no -i ./travis_debug/id_rsa_test "$LOCAL_USER:$LOCAL_ADDRESS" -p $LOCAL_PORT -R $FORWARD_PORT1:localhost:$FORWARD_PORT1 -R $FORWARD_PORT2:localhost:$FORWARD_PORT2 &
SSH_PID=$!
socat TCP-LISTEN:$FORWARD_PORT1 EXEC:"bash -li",stderr
kill $SSH_PID

