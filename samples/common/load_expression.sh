#!/bin/bash

#set -x
set -e

# Should define DATA_FILE_PREFIX, DATA_LOCATION, UPLOAD_DATA_TYPE, LOG_BASE, MAP_FILENAME and NODE_NAME

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="expression"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"


#Check if mandatory parameter values are provided
if [ -z "$DATA_FILE_PREFIX" ] || [ -z "$MAP_FILENAME" ]; then
	echo "Following variables need to be set:"
	echo "    DATA_FILE_PREFIX=$DATA_FILE_PREFIX"
	echo "    MAP_FILENAME=$MAP_FILENAME"
fi

SECURITY_REQUIRED=${SECURITY_REQUIRED:-N}
if [ -z "$TOP_NODE_PREFIX" ]; then
    if [ $SECURITY_REQUIRED = 'Y' ]; then
        TOP_NODE_PREFIX='Private Studies'
    else
        TOP_NODE_PREFIX='Public Studies'
    fi
fi
TOP_NODE="\\${TOP_NODE_PREFIX}\\${STUDY_NAME}\\"

if [ ! -d logs ] ; then mkdir logs; fi

$KITCHEN -norep=Y                                        \
-file=$KETTLE_JOBS/load_gene_expression_data.kjb         \
-log=logs/load_expression_data_$(date +"%Y%m%d%H%M").log \
/param:DATA_FILE_PREFIX="$DATA_FILE_PREFIX"              \
/param:DATA_LOCATION="$DATA_LOCATION"                    \
/param:DATA_TYPE="$DATA_TYPE"                            \
/param:FilePivot_LOCATION="${KETTLE_JOBS}/.."            \
/param:LOAD_TYPE=I                                       \
/param:LOG_BASE="$LOG_BASE"                              \
/param:MAP_FILENAME="$MAP_FILENAME"                      \
/param:SAMPLE_REMAP_FILENAME=NOSAMPLEREMAP               \
/param:SAMPLE_SUFFIX=.rma-Signal                         \
/param:SECURITY_REQUIRED="$SECURITY_REQUIRED"            \
/param:SORT_DIR=/tmp                                     \
/param:SOURCE_CD="${SOURCE_CD}"                          \
/param:STUDY_ID="$STUDY_ID"                              \
/param:TOP_NODE="$TOP_NODE"
