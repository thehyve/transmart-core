#!/bin/bash

#set -x
set -e

echo "running load_expression_annotation.sh $1"

# General optional parameters:
#   DATA_LOCATION, STUDY_NAME, STUDY_ID
# Mandatory parameters specific for this upload script:
#   ANNOTATIONS_FILE
# Optional parameter(s) specific for this upload script:
#   PLATFORM_ID, PLATFORM_TITLE, GENOME_RELEASE

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="annotation"

# Check if mandatory variables are set
if [ -z "$ANNOTATIONS_FILE" ]; then
        echo "Following variables need to be set:"
        echo "    ANNOTATIONS_FILE=$ANNOTATIONS_FILE"
        exit -1
fi

# Check if mandatory parameter values are provided
# Read platform from first line, first column
PLATFORM=$(awk -F'\t' 'BEGIN{getline}{print $1}' "${ANNOTATIONS_FILE}" | head -n 1)
if [ ! -z "$PLATFORM_ID" ]; then
    if [[ "$PLATFORM" != "$PLATFORM_ID" ]]
    then
        echo "Error: PLATFORM_ID=$PLATFORM_ID defined in annotation.params differs from PLATFORM=$PLATFORM defined in $ANNOTATIONS_FILE"
        exit 1
    fi
fi

# Is the platform already uploaded?
ALREADY_LOADED=`$PGSQL_BIN/psql -c "select exists \
                (select platform from deapp.de_gpl_info where platform = '$PLATFORM')" -tA`
if [ $ALREADY_LOADED = 't' ]; then
    echo -e "\e[33mWARNING\e[m: Platform $PLATFORM already loaded; skipping" >&2
    exit 0
fi

# Read organism from first line, last column (must not have DOS line endings)
READ_ORGANISM=$(awk -F'\t' 'BEGIN{getline}{print $NF}' "${ANNOTATIONS_FILE}" | head -n 1)
if [ ! -z "$ORGANISM" ]; then
    if [[ "$READ_ORGANISM" != "$ORGANISM" ]]
    then
        echo "Error: ORGANISM=$ORGANISM defined in annotation.params differs from ORGANISM=$READ_ORGANISM defined in $ANNOTATIONS_FILE"
        exit 1
    fi
fi



PLATFORM_TITLE=${PLATFORM_TITLE:-${PLATFORM}}
$PGSQL_BIN/psql -c "COPY deapp.de_gpl_info(platform, title, organism, marker_type, genome_build) FROM STDIN" <<HEREDOC
$PLATFORM	$PLATFORM_TITLE	$READ_ORGANISM	Gene Expression	$GENOME_RELEASE
HEREDOC

# Upload platform definition
echo "Going to upload platform definition"
$PGSQL_BIN/psql <<_END
	truncate tm_lz.lt_src_deapp_annot;
        \copy tm_lz.lt_src_deapp_annot (gpl_id,probe_id,gene_symbol,gene_id,organism) from '$ANNOTATIONS_FILE' with (FORMAT csv, DELIMITER E'\t', NULL '', HEADER, QUOTE E'\b');
_END

nlines=$($PGSQL_BIN/psql -c "select * from tm_lz.lt_src_deapp_annot" |wc -l)
echo "Number of rows uploaded: $(($nlines - 4))"

RESULT=`$PGSQL_BIN/psql -c "SELECT TM_CZ.i2b2_load_annotation_deapp()" -tA`
#i2b2_load_annotation_deapp() returns 1 on success
if [ $RESULT -ne 1  ]; then
    echo "Call to load function failed; check error/audit tables"
    exit 1
fi
