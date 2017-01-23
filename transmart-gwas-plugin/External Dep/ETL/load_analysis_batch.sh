set -x
fn=${1}
log_date=`date +%Y%m%d%H%M%S`
/usr/local/kettle/data-integration/kitchen.sh -norep=Y -file=Kettle-ETL/process_analysis_files.kjb \
-param:DATA_LOCATION=/old4/23ME/transmart/ETL/Analysis_Metadata/ \
-param:LOAD_TYPE=I \
-param:SORT_DIR=/old4/ETL/Kettle-tmp \
-param:LOADER_PATH=/app/oracle/product/11.2/bin/sqlldr \
-param:METADATA_FILE=$fn
mv /old4/23ME/transmart/ETL/Analysis_Metadata/$fn /old4/23ME/transmart/ETL/Analysis_Metadata/$fn.loaded

