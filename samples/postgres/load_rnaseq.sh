#!/bin/bash
#set -x
#set -e

# General optional parameters:
#   DATA_LOCATION, STUDY_NAME, STUDY_ID
# Specific mandatory parameters for this upload script:
#   RNASEQ_DATA_FILE, SUBJECT_SAMPLE_MAPPING, PLATFORM_FILE
# Specific optional parameters for this upload script:
#   TOP_NODE_PREFIX, SECURITY_REQUIRED, SOURCE_CD 

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="rnaseq"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"

# Check if mandatory parameter values are provided
if [ -z "$RNASEQ_DATA_FILE" ] || [ -z "$SUBJECT_SAMPLE_MAPPING" ] ; then
        echo "Following variables need to be set:"
	echo "    RNASEQ_DATA_FILE=$RNASEQ_DATA_FILE"
	echo "    SUBJECT_SAMPLE_MAPPING=$SUBJECT_SAMPLE_MAPPING"
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

SOURCE_CD=${SOURCE_CD:-STD}

# Upload SubjectSamplMap
  echo "Uploading SubjectSampleMap from: ${SUBJECT_SAMPLE_MAPPING} into the landing-zone"
  $PGSQL_BIN/psql <<_END
    truncate TABLE tm_lz.lt_src_mrna_subj_samp_map;
    \copy tm_lz.lt_src_mrna_subj_samp_map \
        (trial_name,site_id,subject_id,sample_cd,platform,tissue_type,attribute_1,attribute_2,category_cd,source_cd) \
        FROM '${SUBJECT_SAMPLE_MAPPING}' WITH (FORMAT CSV, DELIMITER E'\t', HEADER, QUOTE E'\b');
_END

# Upload data-file into the landing-zone
  echo "Entering data from: ${RNASEQ_DATA_FILE} into the landing-zone"
  $PGSQL_BIN/psql <<_END
    truncate TABLE tm_lz.lt_src_rnaseq_data;
    \copy tm_lz.lt_src_rnaseq_data \
        ( trial_name, region_name, expr_id, readcount, normalized_readcount )  \
        FROM '${RNASEQ_DATA_FILE}' WITH (FORMAT CSV, DELIMITER E'\t', HEADER, QUOTE E'\b');
_END

# transport data from the landing-zone into the transmart tables
  echo "Move data from landing zone into transmart/i2b2 tables"
  $PGSQL_BIN/psql <<_END
    select tm_cz.i2b2_process_rnaseq_data('${STUDY_ID}', '${TOP_NODE}', '${SOURCE_CD}', '${SECURITY_REQUIRED}') 
_END

echo "All done."
