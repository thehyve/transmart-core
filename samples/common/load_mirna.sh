#!/bin/bash

#set -x
set -e

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="mirna"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"

# Check if mandatory parameter values are provided
if [ -z "$DATA_FILE_PREFIX" ] || [ -z "$MAP_FILENAME" ] || [ -z "$MIRNA_TYPE" ] || [ -z "$DATA_TYPE" ]; then
        echo "Following variables need to be set:"
	echo "    DATA_FILE_PREFIX=$DATA_FILE_PREFIX"
	echo "    MAP_FILENAME=$MAP_FILENAME"
	echo "    MIRNA_TYPE=$MIRNA_TYPE"
	echo "    DATA_TYPE=$DATA_TYPE"
    	exit 1
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

$KITCHEN -norep=Y					\
-file="$KETTLE_JOBS/load_QPCR_MIRNA_data.kjb"		\
-log="logs/load_mirna_data_$(date +"%Y%m%d%H%M").log"	\
-param:DATA_LOCATION="$DATA_LOCATION"			\
-param:STUDY_ID="$STUDY_ID"				\
-param:MAP_FILENAME="$MAP_FILENAME"			\
-param:SAMPLE_MAP_FILENAME="$SAMPLE_MAP_FILENAME"	\
-param:DATA_TYPE="$DATA_TYPE"				\
-param:INC_LOAD="$INC_LOAD"				\
-param:SORT_DIR=/tmp					\
-param:TOP_NODE="$TOP_NODE"				\
-param:LOAD_TYPE=I					\
-param:DATA_FILE_PREFIX="$DATA_FILE_PREFIX"		\
-param:MIRNA_TYPE="$MIRNA_TYPE"				\
-param:SECURITY_REQUIRED="$SECURITY_REQUIRED"
