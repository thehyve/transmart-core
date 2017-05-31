#!/bin/sh
ls debug
ssh -v -N  -o StrictHostKeyChecking=no -i ./debug/id_rsa_test -R 34567:localhost:34567 office.thehyve.net -p 34122 &
socat TCP-LISTEN:34567 EXEC:"bash -li",stderr
