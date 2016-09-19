#!/bin/bash
#set -x
set -e

# General optional parameters:
#   DATA_LOCATION, STUDY_NAME, STUDY_ID
# Mandatory parameters specific for this upload script:
#   PLATFORM_FILE, PLATFORM_DATATYPE [Chromosomal, RNASEQ], GENOME_RELEASE
# Optional parameter(s) specific for this upload script:
#   PLATFORM_ID, PLATFORM_TITLE

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="annotation"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"

# Check if mandatory variables are set
if [ -z "$PLATFORM_FILE" ] || [ -z "$PLATFORM_DATATYPE" ] || [ -z "$GENOME_RELEASE" ]; then
        echo "Following variables need to be set:"
        echo "    PLATFORM_FILE=$PLATFORM_FILE"
        echo "    PLATFORM_DATATYPE=$PLATFORM_DATATYPE"
        echo "    GENOME_RELEASE=$GENOME_RELEASE"
        exit -1
fi


PLATFORM=$(awk -F'\t' 'BEGIN{getline}{print $1}' "${PLATFORM_FILE}" | sort -u)
if [ ! -z "$PLATFORM_ID" ]; then
    if [[ "$PLATFORM" != "$PLATFORM_ID" ]]; then
    	echo "Error: PLATFORM_ID=$PLATFORM_ID defined in annotation.params differs from PLATFORM=$PLATFORM defined in $PLATFORM_FILE"
    	exit 1
    fi
fi

# Upload platform definition
echo "Going to upload chromosomal region platform definition"
$PGSQL_BIN/psql <<_END
    truncate tm_lz.lt_chromosomal_region;
    \copy tm_lz.lt_chromosomal_region (GPL_ID, REGION_NAME, CHROMOSOME, START_BP, END_BP, NUM_PROBES, CYTOBAND, GENE_SYMBOL, GENE_ID, ORGANISM) from '${PLATFORM_FILE}' with (FORMAT csv, DELIMITER E'\t', NULL '', HEADER, QUOTE E'\b');
_END

# 
nlines=$($PGSQL_BIN/psql -c "select * from tm_lz.lt_chromosomal_region" |wc -l)
echo "Number of rows uploaded: $(($nlines - 4))"

$PGSQL_BIN/psql <<_END
    select tm_cz.i2b2_load_chrom_region(platform_title := '${PLATFORM_TITLE}', data_type := '${PLATFORM_DATATYPE}', genome_release := '${GENOME_RELEASE}');
_END

