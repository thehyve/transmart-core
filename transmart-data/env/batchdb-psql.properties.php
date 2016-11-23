<?php require __DIR__ . '/../lib/php/env_helper.inc.php' ?>
batch.jdbc.driver=org.postgresql.Driver
batch.jdbc.url=jdbc:postgresql://<?= $host ?>:<?= $_ENV['PGPORT'] ?>/<?= $_ENV['PGDATABASE'], "\n" ?>
batch.jdbc.user=tm_cz
batch.jdbc.password=<?= get_env('TM_CZ_PWD', 'tm_cz'), "\n" ?>
