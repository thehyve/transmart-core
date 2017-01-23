<?php
function get($key, $default) {
	return isset($_ENV[$key]) ? $_ENV[$key] : $default;
}
?>
TM_CZ_DB_NAME=<?= $_ENV['ORASID'], "\n" ?>
TM_CZ_DB_PORT=<?= $_ENV['ORAPORT'], "\n" ?>
TM_CZ_DB_PWD=<?= get('TM_CZ_PWD', 'tm_cz'), "\n" ?>
TM_CZ_DB_SERVER=<?= $_ENV['ORAHOST'], "\n" ?>
TM_CZ_DB_USER=tm_cz
TM_LZ_DB_NAME=<?= $_ENV['ORASID'] , "\n" ?>
TM_LZ_DB_PORT=<?= $_ENV['ORAPORT'] , "\n" ?>
TM_LZ_DB_PWD=<?= get('TM_LZ_PWD', 'tm_lz'), "\n" ?>
TM_LZ_DB_SERVER=<?= $_ENV['ORAHOST'], "\n" ?>
TM_LZ_DB_USER=tm_lz
TM_WZ_DB_NAME=<?= $_ENV['ORASID'] , "\n" ?>
TM_WZ_DB_PORT=<?= $_ENV['ORAPORT'] , "\n" ?>
TM_WZ_DB_PWD=<?= get('TM_WZ_PWD', 'tm_wz'), "\n" ?>
TM_WZ_DB_SERVER=<?= $_ENV['ORAHOST'], "\n" ?>
TM_WZ_DB_USER=tm_wz
DEAPP_DB_NAME=<?= $_ENV['ORASID'] , "\n" ?>
DEAPP_DB_PORT=<?= $_ENV['ORAPORT'] , "\n" ?>
DEAPP_DB_SERVER=<?= $_ENV['ORAHOST'], "\n" ?>
DEAPP_DB_PWD=<?= get('DEAPP_PWD', 'deapp'), "\n" ?>
DEAPP_DB_USER=deapp
DEAPP_PWD=<?= get('DEAPP_PWD', 'deapp'), "\n" ?>
DEAPP_USER=deapp
BIOMART_DB_NAME=<?= $_ENV['ORASID'] , "\n" ?>
BIOMART_DB_PORT=<?= $_ENV['ORAPORT'] , "\n" ?>
BIOMART_DB_SERVER=<?= $_ENV['ORAHOST'], "\n" ?>
BIOMART_DB_PWD=<?= get('BIOMART_PWD', 'biomart'), "\n" ?>
BIOMART_DB_USER=biomart
BIOMART_PWD=<?= get('BIOMART_PWD', 'biomart'), "\n" ?>
BIOMART_USER=biomart
BIOMART_STAGE_DB_NAME=<?= $_ENV['ORASID'] , "\n" ?>
BIOMART_STAGE_DB_PORT=<?= $_ENV['ORAPORT'] , "\n" ?>
BIOMART_STAGE_DB_SERVER=<?= $_ENV['ORAHOST'], "\n" ?>
BIOMART_STAGE_DB_PWD=<?= get('BIOMART_STAGE_PWD', 'biomart_stage'), "\n" ?>
BIOMART_STAGE_DB_USER=biomart_stage
I2B2DEMODATA_DB_NAME=<?= $_ENV['ORASID'] , "\n" ?>
I2B2DEMODATA_DB_PORT=<?= $_ENV['ORAPORT'] , "\n" ?>
I2B2DEMODATA_DB_SERVER=<?= $_ENV['ORAHOST'], "\n" ?>
I2B2DEMODATA_DB_PWD=<?= get('I2B2DEMODATA_PWD', 'i2b2demodata'), "\n" ?>
I2B2DEMODATA_DB_USER=i2b2demodata
