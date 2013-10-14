<?php
if (!isset($_ENV['TRANSMART_USER'])) {
        fprintf(STDERR, "TRANSMART_USER is not set\n");
        exit(1);
}
$u = $_ENV['TRANSMART_USER'];
$r = __DIR__ . '/root/bin/R';
?>
#!/bin/sh

### BEGIN INIT INFO
# Provides:             rserve
# Required-Start:       $local_fs $remote_fs $network
# Required-Stop:        $local_fs $remote_fs $network
# Default-Start:        2 3 4 5
# Default-Stop:         0 1 6
# Short-Description:    rserve
### END INIT INFO

case "$1" in
        start)
                su - -c "<?= $r ?> CMD Rserve --quiet --vanilla" <?= $u, "\n" ?>
        ;;

        stop)
                if pgrep -u <?= $u ?> -f Rserve  > /dev/null
                then
                    kill `pgrep -u  <?= $u ?> -f Rserve`
                else
                    echo "nothing to stop; Rserve is not running"
                    exit 0
                fi
        ;;

        restart|reload|force-reload)
                kill `pgrep -u <?= $u ?> -f Rserve`
                su - -c "<?= $r ?> CMD Rserve --vanilla" <?= $u, "\n" ?>
        ;;

        status)
                if pgrep -u <?= $u ?> -f Rserve > /dev/null
                then
                    echo "Rserve service running."
                    exit 0
                else
                    echo "Rserve is not running"
                    exit 1
                fi
        ;;
          *)
                echo "Usage: $0 {start|stop|status|restart|force-reload|reload}" >&2
                exit 1
        ;;
esac
