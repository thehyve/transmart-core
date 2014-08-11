<?php require __DIR__ . '/../../lib/php/env_helper.inc.php'
function get($key, $default) {
	return isset($_ENV[$key]) ? $_ENV[$key] : $default;
}
?>
TM_CZ_DB_NAME=<?= $_ENV['PGDATABASE'], "\n" ?>
TM_CZ_DB_PORT=<?= $_ENV['PGPORT'], "\n" ?>
TM_CZ_DB_PWD=<?= get('TM_CZ_PWD', 'tm_cz') ?>
TM_CZ_DB_SERVER=<?= $host, "\n" ?>
TM_CZ_DB_USER=tm_cz
TM_LZ_DB_NAME=<?= $_ENV['PGDATABASE'] , "\n" ?>
TM_LZ_DB_PORT=<?= $_ENV['PGPORT'] , "\n" ?>
TM_LZ_DB_PWD=<?= get('TM_LZ_PWD', 'tm_lz') ?>
TM_LZ_DB_SERVER=<?= $host, "\n" ?>
TM_LZ_DB_USER=tm_lz
TM_WZ_DB_NAME=<?= $_ENV['PGDATABASE'] , "\n" ?>
TM_WZ_DB_PORT=<?= $_ENV['PGPORT'] , "\n" ?>
TM_WZ_DB_PWD=<?= get('TM_WZ_PWD', 'tm_wz') ?>
TM_WZ_DB_SERVER=<?= $host, "\n" ?>
TM_WZ_DB_USER=tm_wz
DEAPP_DB_NAME=<?= $_ENV['PGDATABASE'] , "\n" ?>
DEAPP_DB_PORT=<?= $_ENV['PGPORT'] , "\n" ?>
DEAPP_DB_SERVER=<?= $host, "\n" ?>
DEAPP_PWD=<?= get('DEAPP_PWD', 'deapp') ?>
DEAPP_USER=deapp
BIOMART_DB_NAME=<?= $_ENV['PGDATABASE'] , "\n" ?>
BIOMART_DB_PORT=<?= $_ENV['PGPORT'] , "\n" ?>
BIOMART_DB_SERVER=<?= $host, "\n" ?>
BIOMART_PWD=<?= get('BIOMART_PWD', 'biomart') ?>
BIOMART_USER=biomart
