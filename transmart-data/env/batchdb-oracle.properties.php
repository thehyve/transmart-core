<?php require __DIR__ . '/../lib/php/env_helper.inc.php' ?>
batch.jdbc.driver=oracle.jdbc.driver.OracleDriver
batch.jdbc.url=jdbc:oracle:thin:@<?= $_ENV['ORAHOST']  ?>:<?= $_ENV['ORAPORT'] ?>:<?= $_ENV['ORASID'], "\n" ?>
batch.jdbc.user=tm_cz
batch.jdbc.password=<?= get_env('TM_CZ_PWD', 'tm_cz'), "\n" ?>
