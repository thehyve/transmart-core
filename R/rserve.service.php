<?php
if (!isset($_ENV['TRANSMART_USER'])) {
        fprintf(STDERR, "TRANSMART_USER is not set\n");
        exit(1);
}
$u = $_ENV['TRANSMART_USER'];
$r = __DIR__ . '/root/bin/R';
$c = __DIR__ . '/Rserv_nodaemon.conf'
?>
[Unit]
Description=Rserve (TCP/IP server for running R expressions)
Documentation=http://rforge.net/Rserve/

[Service]
ExecStart=<?= $r ?> CMD Rserve --quiet --vanilla --RS-conf <?= $c, "\n" ?>
User=<?= $u, "\n" ?>
TimeoutSec=15s
Restart=always
Nice=19
StandardOutput=null
# test this for production:
#ReadOnlyDirectories=/
#ReadWriteDirectories=/var/cache/jobs
#PrivateTmp=true

[Install]
WantedBy=multi-user.target
