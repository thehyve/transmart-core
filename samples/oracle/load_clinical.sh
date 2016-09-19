#!/bin/bash

#set -x
set -e

# General optional parameters:
#   DATA_LOCATION, STUDY_NAME, STUDY_ID
# Specific mandatory parameters for this upload script:
#   COLUMN_MAP_FILE, WORD_MAP_FILE, either R_JOBS_PSQL or KETTLE_JOBS_PSQL
# Specific optional parameters for this upload script:
#   TOP_NODE_PREFIX, SECURITY_REQUIRED, USE_R_UPLOAD

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="clinical"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"

# Check if mandatory variables are set
if [ -z "$STUDY_ID" ] || [ -z "$COLUMN_MAP_FILE" ]; then
	echo "Following variables need to be set:"
	echo "    STUDY_ID=$STUDY_ID"
	echo "    COLUMN_MAP_FILE=$COLUMN_MAP_FILE"
	exit -1
fi

if [ -z "$KETTLE_HOME" ]; then
	echo "KETTLE_HOME is not set"
	exit 1
fi

if [ -z "$KETTLE_JOBS" ]; then
	echo "KETTLE_JOBS is not set"
	exit 1
fi

    mkdir -p logs

    CLINICAL_JOB='create_clinical_data.kjb'
    if [ ! -z "$INC_LOAD" ]; then
	if [ "$INC_LOAD" = 'Y' ]; then
	    CLINICAL_JOB='increment_clinical_data.kjb'
	fi
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

$KITCHEN -norep=Y                                      \
-file=$KETTLE_JOBS/$CLINICAL_JOB            \
-log=logs/load_clinical_data_$(date +"%Y%m%d%H%M").log \
-param:COLUMN_MAP_FILE="$COLUMN_MAP_FILE"              \
-param:DATA_LOCATION="$DATA_LOCATION"                  \
-param:LOAD_TYPE=I                                     \
-param:SECURITY_REQUIRED="$SECURITY_REQUIRED"          \
-param:SORT_DIR=/tmp                                   \
-param:STUDY_ID="$STUDY_ID"                            \
-param:TOP_NODE="$TOP_NODE"                            \
-param:WORD_MAP_FILE="$WORD_MAP_FILE"                  \
-param:RECORD_EXCLUSION_FILE="$RECORD_EXCLUSION_FILE"
#-param:SQLLDR_PATH=/spin/pg/master/bin/psql \
