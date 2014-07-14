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

# Should define DATA_FILE_PREFIX, DATA_LOCATION, DATA_TYPE, LOG_BASE, MAP_FILENAME and NODE_NAME
# SOURCE_CD is optional
source $1

#FilePivot.jar expected in $KETTLE_JOBS/..

SECURITY_REQUIRED=${SECURITY_REQUIRED:-N}

if [ $SECURITY_REQUIRED = 'Y' ]; then
    TOP_NODE_PREFIX='Private Studies'
else
    TOP_NODE_PREFIX='Public Studies'
fi

TOP_NODE='\'$TOP_NODE_PREFIX'\'$STUDY_ID'\'$NODE_NAME 

$KITCHEN -norep=Y                                        \
-file=$KETTLE_JOBS/load_gene_expression_data.kjb         \
-log=logs/load_expression_data_$(date +"%Y%m%d%H%M").log \
/param:DATA_FILE_PREFIX="$DATA_FILE_PREFIX"              \
/param:DATA_LOCATION="$DATA_LOCATION"                    \
/param:DATA_TYPE="$DATA_TYPE"                            \
/param:FilePivot_LOCATION="${KETTLE_JOBS}.."             \
/param:LOAD_TYPE=I                                       \
/param:LOG_BASE="$LOG_BASE"                              \
/param:MAP_FILENAME="$MAP_FILENAME"                      \
/param:SAMPLE_REMAP_FILENAME=NOSAMPLEREMAP               \
/param:SAMPLE_SUFFIX=.rma-Signal                         \
/param:SECURITY_REQUIRED="$SECURITY_REQUIRED"            \
/param:SORT_DIR=/tmp                                     \
/param:SOURCE_CD="${SOURCE_CD:-STD}"                               \
/param:STUDY_ID="$STUDY_ID"                              \
/param:TOP_NODE="$TOP_NODE"
