#!/bin/bash

set -x
set -e

if [ -z "$KETTLE_HOME" ]; then
        echo "KETTLE_HOME is not set"
        exit 1
fi

if [ -z "$KETTLE_JOBS" ]; then
        echo "KETTLE_JOBS is not set"
        exit 1
fi

# $KETTLE_HOME should have been set by the caller
export KETTLE_HOME

# Should define MAP_FILENAME, DATA_TYPE, COLUMN_MAPPING_FILE AND DATA_FILE_PREFIX
source $1

SECURITY_REQUIRED=${SECURITY_REQUIRED:-N}

if [ $SECURITY_REQUIRED = 'Y' ]; then
    TOP_NODE_PREFIX='Private Studies'
else
    TOP_NODE_PREFIX='Public Studies'
fi

$KITCHEN -norep=Y										\
-file="$KETTLE_JOBS/load_QPCR_MIRNA_data.kjb"			\
-log="load_mirna_data_$(date +"%Y%m%d%H%M").log"		\
-param:DATA_LOCATION="$DATA_LOCATION"					\
-param:STUDY_ID="$STUDY_ID"								\
-param:MAP_FILENAME="$MAP_FILENAME"						\
-param:DATA_TYPE="$DATA_TYPE"							\
-param:SORT_DIR=/tmp									\
-param:TOP_NODE='\'"$TOP_NODE_PREFIX"'\'$STUDY_ID'\'	\
-param:LOAD_TYPE=I										\
-param:DATA_FILE_PREFIX="$DATA_FILE_PREFIX"				\
-param:DATA_FILE="$DATA_FILE"							\
-param:MIRNA_TYPE="$MIRNA_TYPE"							\
-param:SECURITY_REQUIRED="$SECURITY_REQUIRED"
