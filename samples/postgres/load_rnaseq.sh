#!/bin/bash
#set -x
#set -e

# General optional parameters:
#   DATA_LOCATION, STUDY_NAME, STUDY_ID
# Specific mandatory parameters for this upload script:
#   RNASEQ_DATA_FILE, SUBJECT_SAMPLE_MAPPING, R_JOBS_PSQL or KETTLE_JOBS_PSQL
# Specific optional parameters for this upload script:
#   TOP_NODE_PREFIX, SECURITY_REQUIRED, SOURCE_CD, DATA_TYPE

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="rnaseq"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"

if [ -z "$RNASEQ_TYPE" ]; then
    RNASEQ_TYPE=ACGH
else
    if [ "$RNASEQ_TYPE" != "RNASEQ" ] && [ "$RNASEQ_TYPE" != "ACGH" ]; then
       echo "Invalid RNASEQ_TYPE parameter $RNASEQ_TYPE"
       echo "possible values are RNASEQ or ACGH"
       exit 1
    fi
fi

if [ -z "$R_JOBS_PSQL" ]; then
    if [ -z "$KETTLE_JOBS_PSQL" ]; then
        echo "Error: Neither R_JOBS_PSQL nor KETTLE_JOBS_PSQL parameter has been set"
        exit 1
    else
        R_JOBS_PSQL="${KETTLE_JOBS_PSQL}/../../R"
    fi
fi

# Check if mandatory parameter values are provided
if [ -z "$RNASEQ_DATA_FILE" ] || [ -z "$SUBJECT_SAMPLE_MAPPING" ] ; then
    echo "Following variables need to be set:"
    echo "    RNASEQ_DATA_FILE=$RNASEQ_DATA_FILE"
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


if [ "$RNASEQ_TYPE" = "RNASEQ" ]; then
    if [ ! -d logs ] ; then mkdir logs; fi

    $KITCHEN -norep=Y                                        \
	     -file=$KETTLE_JOBS/load_RNA_sequencing_data.kjb          \
	     -log=logs/load_rnaseq_data_$(date +"%Y%m%d%H%M").log     \
	     /param:DATA_FILE_PREFIX="$DATA_FILE_PREFIX"              \
	     /param:DATA_LOCATION="$DATA_LOCATION"                    \
	     /param:DATA_TYPE="$DATA_TYPE"                            \
	     /param:FilePivot_LOCATION="${KETTLE_JOBS}/.."            \
	     /param:INC_LOAD="$INC_LOAD"                              \
	     /param:LOAD_TYPE=I                                       \
	     /param:LOG_BASE="$LOG_BASE"                              \
	     /param:MAP_FILENAME="$SUBJECT_SAMPLE_MAPPING"            \
	     /param:SAMPLE_MAP_FILENAME="$SAMPLE_MAP_FILENAME"        \
	     /param:SAMPLE_SUFFIX=                                    \
	     /param:SECURITY_REQUIRED="$SECURITY_REQUIRED"            \
	     /param:SORT_DIR=/tmp                                     \
	     /param:SOURCE_CD="${SOURCE_CD}"                          \
	     /param:STUDY_ID="$STUDY_ID"                              \
	     /param:TOP_NODE="$TOP_NODE"

    echo "All done."
    exit 0
fi

RSCRIPT="Rscript"
if ! type "$RSCRIPT" 2>&1 > /dev/null; then
    RSCRIPT="/opt/R/bin/Rscript"
    if ! type "$RSCRIPT" > /dev/null; then
        echo "Error: Rscript command not found"
        exit 1
    fi
fi

# The unpivoted-file which will be loaded into the database
RNASEQ_DATA_FILE_UPLOAD="${RNASEQ_DATA_FILE}".upload

# Create the unpivoted file to be loaded into the database.
echo "Start re-arranging input..."
${RSCRIPT} ${R_JOBS_PSQL}/RNASeq/unpivot_RNASeq_data.R studyID=${STUDY_ID} \
                                                       RNASeqFile="${RNASEQ_DATA_FILE}" \
                                                       dataOUT="${RNASEQ_DATA_FILE_UPLOAD}"

echo "unpivoted input stored in file: ${RNASEQ_DATA_FILE_UPLOAD}"
echo ""

# Upload SubjectSamplMap
  echo "Uploading SubjectSampleMap from: ${SUBJECT_SAMPLE_MAPPING} into the landing-zone"
  $PGSQL_BIN/psql <<_END
    truncate TABLE tm_lz.lt_src_mrna_subj_samp_map;
    \copy tm_lz.lt_src_mrna_subj_samp_map \
        (trial_name,site_id,subject_id,sample_cd,platform,tissue_type,attribute_1,attribute_2,category_cd,source_cd) \
        FROM '${SUBJECT_SAMPLE_MAPPING}' WITH (FORMAT CSV, DELIMITER E'\t', HEADER, QUOTE E'\b');
_END

# Upload data-file into the landing-zone
  echo "Entering data from: '${RNASEQ_DATA_FILE_UPLOAD}' into the landing-zone"
  $PGSQL_BIN/psql <<_END
    truncate TABLE tm_lz.lt_src_rnaseq_data;
    \copy tm_lz.lt_src_rnaseq_data \
        ( trial_name, region_name, expr_id, readcount, normalized_readcount )  \
        FROM '${RNASEQ_DATA_FILE_UPLOAD}' WITH (FORMAT CSV, DELIMITER E'\t', HEADER, QUOTE E'\b');
_END

# transport data from the landing-zone into the transmart tables
  echo "Move data from landing zone into transmart/i2b2 tables"
  $PGSQL_BIN/psql <<_END
    select tm_cz.i2b2_process_rnaseq_data('${STUDY_ID}', '${TOP_NODE}', '${SOURCE_CD}', '${SECURITY_REQUIRED}', '${DATA_TYPE}')
_END

echo "All done."
