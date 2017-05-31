#!/bin/sh
ls debug
chmod go-rwx debug/id_rsa_test
ssh -v -N  -o StrictHostKeyChecking=no -i ./debug/id_rsa_test -R 34567:localhost:34567 jan@office.thehyve.net -p 34122 &
SSH_PID=$!
socat TCP-LISTEN:34567 EXEC:"bash -li",stderr
kill SSH_PID

