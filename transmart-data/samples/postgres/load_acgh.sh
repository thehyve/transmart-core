#!/bin/bash

#set -x
set -e

# General optional parameters:
#   DATA_LOCATION, STUDY_NAME, STUDY_ID
# Specific mandatory parameters for this upload script:
#   DATA_FILE_PREFIX, MAP_FILENAME
# Specific optional parameters for this upload script:
#   TOP_NODE_PREFIX, SECURITY_REQUIRED, SOURCE_CD 

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="acgh"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"

# Check if mandatory parameter values are provided

if [ -z "$STUDY_ID" ] || [ -z "$DATA_FILE_PREFIX" ] || [ -z "$MAP_FILENAME" ] ; then
        echo "Following variables need to be set:"
	echo "    STUDY_ID=$STUDY_ID"
	echo "    DATA_FILE_PREFIX=$DATA_FILE_PREFIX"
	echo "    MAP_FILENAME=$MAP_FILENAME"
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

TEMPDIR=$(mktemp -d -t load_acgh_XXXXXX)
trap 'rm -rf $TEMPDIR' EXIT

if [ ! -d logs ] ; then mkdir logs; fi

# Upload the chromosomal data
  echo "Uploading the chromosomal data"
  $KITCHEN -norep=Y                                                        \
	-file=$KETTLE_JOBS/load_acgh_data.kjb                              \
	-log='logs/load_'$STUDY_ID'_acgh_data_'$(date +"%Y%m%d%H%M")'.log' \
	-param:DATA_FILE_PREFIX="$DATA_FILE_PREFIX"                        \
	-param:DATA_LOCATION="$DATA_LOCATION"                              \
	-param:FilePivot_LOCATION=$KETTLE_JOBS'../'                        \
	-param:LOAD_TYPE=I                                                 \
	-param:SAMPLE_REMAP_FILENAME=NOSAMPLEREMAP                         \
	-param:SAMPLE_SUFFIX=.chip                                         \
	-param:MAP_FILENAME="$MAP_FILENAME"                                \
	-param:SECURITY_REQUIRED=$SECURITY_REQUIRED                        \
	-param:SORT_DIR="$TEMPDIR"                                         \
	-param:SOURCE_CD="${SOURCE_CD:-STD}"                               \
	-param:STUDY_ID=$STUDY_ID					   \
	-param:TOP_NODE="$TOP_NODE"

