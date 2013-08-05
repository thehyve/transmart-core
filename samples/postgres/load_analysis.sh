#!/bin/bash

set -x
set -e

if [ -z "$KETTLE_HOME" ]; then
	echo "KETTLE_HOME is not set"
	exit 1
fi
if [ -z "$DATA_LOCATION" ]; then
	echo "DATA_LOCATION is not set"
	exit 1
fi

# $KETTLE_HOME should have been set by the caller
export KETTLE_HOME

# Should define ANALYSIS_DATA_FILENAME, ANALYSIS_FILENAME, COHORTS_FILENAME,
# SAMPLES_FILENAME, STUDY_DATA_CATEGORY AND STUDY_DISPLAY_CATEGORY
source $1

# the ETL script insist on interpreting LOG_FILENAME as a relative path...
if [ -h $DATA_LOCATION/logs ]; then
	rm $DATA_LOCATION/logs
fi
ln -s `pwd`/logs $DATA_LOCATION/logs

$KITCHEN -norep=Y                                                \
-file=$KETTLE_JOBS/ETL.search.process_analysis_data.kjb          \
-log=logs/search.process_analysis_data_$(date +"%Y%m%d%H%M").log \
-param:DATA_LOCATION=$DATA_LOCATION                              \
-param:ANALYSIS_DATA_FILENAME=$ANALYSIS_DATA_FILENAME            \
-param:ANALYSIS_FILENAME=$ANALYSIS_FILENAME                      \
-param:COHORTS_FILENAME=$COHORTS_FILENAME                        \
-param:LOG_FILENAME=logs/search.process_analysis_data_e          \
-param:SAMPLES_FILENAME=$SAMPLES_FILENAME                        \
-param:STUDY_ID=$STUDY_ID                                        \
-param:SORT_DIR=/tmp                                             \
-param:STUDY_DATA_CATEGORY=$STUDY_DATA_CATEGORY                  \
-param:STUDY_DISPLAY_CATEGORY=$STUDY_DISPLAY_CATEGORY

echo -e '\e[32mDONE\e[m Do not forget to reindex Solr'

rm $DATA_LOCATION/logs
