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

SECURITY_REQUIRED=${SECURITY_REQUIRED:-N}
if [ -z "$TOP_NODE_PREFIX" ]; then
    if [ $SECURITY_REQUIRED = 'Y' ]; then
        TOP_NODE_PREFIX='Private Studies'
    else
        TOP_NODE_PREFIX='Public Studies'
    fi
fi
TOP_NODE="\\${TOP_NODE_PREFIX}\\${STUDY_NAME}\\"

USE_R_UPLOAD=${USE_R_UPLOAD:-N} # By default use Kettle upload. R upload supports more features, but is currently only available for Postgres

if [ $USE_R_UPLOAD = 'N' ]; then

    if [ -z "$KETTLE_JOBS" ]; then
        echo "Error: KETTLE_JOBS parameter has not been set"
        exit 1
    fi

    mkdir -p logs

    CLINICAL_JOB='create_clinical_data.kjb'
    if [ ! -z "$INC_LOAD" ]; then
	if [ "$INC_LOAD" = 'Y' ]; then
	    CLINICAL_JOB='increment_clinical_data.kjb'
	fi
    fi

    $KITCHEN -norep=Y                                               \
             -file=$KETTLE_JOBS_PSQL/$CLINICAL_JOB                  \
             -log=logs/load_clinical_data_$(date +"%Y%m%d%H%M").log \
             -param:COLUMN_MAP_FILE="$COLUMN_MAP_FILE"              \
             -param:DATA_LOCATION="$DATA_LOCATION"                  \
             -param:LOAD_TYPE=I                                     \
             -param:SECURITY_REQUIRED="$SECURITY_REQUIRED"          \
             -param:SORT_DIR=/tmp                                   \
             -param:STUDY_ID="$STUDY_ID"                            \
             -param:TOP_NODE="$TOP_NODE"                             \
             -param:WORD_MAP_FILE="$WORD_MAP_FILE"                  \
             -param:RECORD_EXCLUSION_FILE="$RECORD_EXCLUSION_FILE"
             #-param:SQLLDR_PATH=/spin/pg/master/bin/psql \

else

    if [ -z "$R_JOBS_PSQL" ]; then
        if [ -z "$KETTLE_JOBS_PSQL" ]; then
            echo "Error: Neither R_JOBS_PSQL nor KETTLE_JOBS_PSQL parameter has been set"
            exit 1
        else
            R_JOBS_PSQL="${KETTLE_JOBS_PSQL}/../../R"
        fi
    fi

    RSCRIPT="Rscript"
    if ! type "$RSCRIPT" 2>&1 > /dev/null; then
        RSCRIPT="/opt/R/bin/Rscript"
        if ! type "$RSCRIPT" > /dev/null; then
            echo "Error: Rscript command not found"
            exit 1
        fi
    fi

    # The pivot-file which will be uploaded into the database
    ClinicalDataUpload="ClinicalData_tm_lz.tsv.uploaded"

    echo "Start re-arranging input..."
    ${RSCRIPT} ${R_JOBS_PSQL}/clinical/load_clinical_data.R studyID=${STUDY_ID} \
                                                            columnMapFile=${COLUMN_MAP_FILE} \
                                                            wordMapFile=${WORD_MAP_FILE} \
                                                            outputFile=${ClinicalDataUpload}
    echo "re-arranged input stored in file: ${ClinicalDataUpload}"
    echo ""

    echo "Start uploading file: ${ClinicalDataUpload} into database (tm_lz.lt_src_clinical_data)"
    $PGSQL_BIN/psql <<_END
       truncate TABLE tm_lz.lt_src_clinical_data;
       \copy tm_lz.lt_src_clinical_data \
           (study_id,site_id,subject_id,visit_name,data_label,modifier_cd,data_value,units_cd,date_timestamp,category_cd,ctrl_vocab_code) \
           FROM '${ClinicalDataUpload}' WITH (FORMAT CSV, DELIMITER E'\t', HEADER, QUOTE E'\b');
       select count(*) from tm_lz.lt_src_clinical_data;
_END

    echo "File: ${ClinicalDataUpload} uploaded into database"
    echo ""

    echo "Call stored-procedure to put data into i2b2 tables"
    $PGSQL_BIN/psql <<_END
        select tm_cz.i2b2_load_clinical_data('${STUDY_ID}', '${TOP_NODE}', '${SECURITY_REQUIRED}') 
_END

fi

echo "All done."
