<?php require __DIR__ . '/../../lib/php/env_helper.inc.php' ?>
driver_class=org.postgresql.Driver
url=jdbc:postgresql://<?= $host ?>:<?= $_ENV['PGPORT'] ?>/<?= $_ENV['PGDATABASE'], "\n" ?>
biomart_username=biomart
biomart_password=<?= get_env('BIOMART_PWD', 'biomart'), "\n" ?>
deapp_username=deapp
deapp_password=<?= get_env('DEAPP_PWD', 'deapp'), "\n" ?>
searchapp_username=searchapp
searchapp_password=<?= get_env('SEARCHAPP_PWD', 'searchapp'), "\n" ?>
