#!/bin/bash

set -x
set -e

if [ -z "$KETTLE_HOME"]; then
	echo "KETTLE_HOME is not set"
	exit 1
fi

# $KETTLE_HOME should have been set by the caller
export KETTLE_HOME

# Should define COLUMN_MAP_FILE, WORD_MAP_FILE AND RECORD_EXCLUSION_FILE
source $1

$KITCHEN -norep=Y                                 \
-file=$KETTLE_JOBS/create_clinical_data.kjb       \
-log=logs/load_clinical_data_$(date +"%Y%m%d%H%M").log \
-param:COLUMN_MAP_FILE=$COLUMN_MAP_FILE           \
-param:DATA_LOCATION=$DATA_LOCATION               \
-param:LOAD_TYPE=I                                \
-param:SECURITY_REQUIRED=N                        \
-param:SORT_DIR=/tmp                              \
-param:STUDY_ID=$STUDY_ID                         \
-param:TOP_NODE='\Public Studies\'$STUDY_ID'\'    \
-param:WORD_MAP_FILE=$WORD_MAP_FILE               \
-param:RECORD_EXCLUSION_FILE=$RECORD_EXCLUSION_FILE
#-param:SQLLDR_PATH=/spin/pg/master/bin/psql \
