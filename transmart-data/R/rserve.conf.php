<?php
$p = $_ENV['R_PREFIX'];
$c = $_ENV['RSERVE_CONF'];
?>

description "Rserve (TCP/IP server for running R expressions)"

start on runlevel [2345]
stop on runlevel [!2345]

pre-start script
    if [ -f /etc/default/rserve ]; then
       . /etc/default/rserve
    fi
    if [ -z "$USER" ]; then
       echo '$USER not defined' >&2
       stop; exit 0;
    fi
end script

script
    if [ -f /etc/default/rserve ]; then
       . /etc/default/rserve
    fi
    exec sudo -u "$USER" '<?= "$p/bin/R" ?>' CMD Rserve --quiet --vanilla --RS-conf '<?= "$c" ?>'
end script

respawn
