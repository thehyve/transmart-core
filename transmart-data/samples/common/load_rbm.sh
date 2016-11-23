#!/bin/bash
#set -x
#set -e

# General optional parameters:
#   DATA_LOCATION, STUDY_NAME, STUDY_ID
# Specific mandatory parameters for this upload script:
#   SUBJECT_SAMPLE_MAPPING, R_JOBS_PSQL or KETTLE_JOBS_PSQL
# Specific optional parameters for this upload script:
#   TOP_NODE_PREFIX, SECURITY_REQUIRED, SOURCE_CD, DATA_TYPE

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="rbm"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"

# Check if mandatory parameter values are provided
if [ -z "$DATA_FILENAME" ] || [ -z "$SUBJECT_SAMPLE_MAPPING" ] ; then
    echo "Following variables need to be set:"
    echo "    DATA_FILENAME=$DATA_FILENAME"
    echo "    SUBJECT_SAMPLE_MAPPING=$SUBJECT_SAMPLE_MAPPING"
    exit 1
fi

# Extract STUDY_ID from subject sample mapping file
STUDY_ID_FROM_SSM=$(awk -F'\t' 'BEGIN{getline}{print $1}' "${SUBJECT_SAMPLE_MAPPING}" | sort -u | tr 'a-z' 'A-Z')
if [ -z "$STUDY_ID_FROM_SSM" ]; then
    echo "Error $0: No STUDY_ID provided in first column of subject sample mapping file $SUBJECT_SAMPLE_MAPPING"
    exit 1
fi

# Check consistent use of STUDY_ID (if provided both as a parameter and in the subject sample mapping file)
if [ -z "$STUDY_ID" ]; then
    STUDY_ID=$STUDY_ID_FROM_SSM
else
    if [[ "$STUDY_ID" != "$STUDY_ID_FROM_SSM" ]]
    then
        echo "Error $0: STUDY_ID=$STUDY_ID defined in params differs from STUDY_ID=$STUDY_ID_FROM_SSM defined in $SUBJECT_SAMPLE_MAPPING"
        exit 1
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

DATA_TYPE=${DATA_TYPE:-R}    # Normalized readcounts are loaded as R (raw) by default. Use L (log2) to load log2 normalized readcounts.
INC_LOAD=${INC_LOAD:-N}
SOURCE_CD=${SOURCE_CD:-STD}
LOG_BASE=${LOG_BASE:-2}

if [ ! -d logs ] ; then mkdir logs; fi

$KITCHEN -norep=Y                                                 \
	 -file=$KETTLE_JOBS/load_rbm_data.kjb                     \
	 -log=logs/load_rbm_data_$(date +"%Y%m%d%H%M").log     \
	 /param:DATA_FILENAME="$DATA_FILENAME"                    \
	 /param:DATA_LOCATION="$DATA_LOCATION"                    \
	 /param:DATA_TYPE="$DATA_TYPE"                            \
	 /param:INC_LOAD="$INC_LOAD"                              \
	 /param:LOAD_TYPE=L                                       \
	 /param:LOG_BASE="$LOG_BASE"                              \
	 /param:MAP_FILENAME="$SUBJECT_SAMPLE_MAPPING"            \
	 /param:SAMPLE_MAP_FILENAME="$SUBJECT_SAMPLE_MAPPING"     \
	 /param:SAMPLE_SUFFIX=                                    \
	 /param:SECURITY_REQUIRED="$SECURITY_REQUIRED"            \
	 /param:SORT_DIR=/tmp                                     \
	 /param:SOURCE_CD="${SOURCE_CD}"                          \
	 /param:STUDY_ID="$STUDY_ID"                              \
	 /param:TOP_NODE="$TOP_NODE"

echo "All done"
