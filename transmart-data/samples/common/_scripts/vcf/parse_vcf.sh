#!/bin/bash

set -e

# This script is used to parse VCF data for a specific study
# The script requires 1 parameter: the params file to be used
# If the parameter is not given, the user is expected to set
# the parameters from the params file himself.
if [ $# -ge 1 ]
	then
		source $1
fi

# DATA_LOCATION can be set to the root directory to search the VCF_FILE
# and SUBJECT_SAMPLE_MAPPING_FILE in. If not set, the files must be specified
# as absolute paths
if [ -z "$DATA_LOCATION" ]; then
	DATA_LOCATION=""
fi

DIR=`dirname $0`

# Make sure the output directory exists
mkdir -p "$VCF_TEMP_DIR"

perl $DIR/parseVCFintoTextFiles.pl "$DATA_LOCATION/$VCF_FILE" "$VCF_TEMP_DIR" "$DATASOURCE" \
		"$DATASET_ID" "$GPL_ID" "$GENOME_BUILD" "$ETL_USER"
perl $DIR/convertMappingIntoSQLFiles.pl "$DATA_LOCATION/$SUBJECT_SAMPLE_MAPPING_FILE" "$VCF_TEMP_DIR" \
		"$STUDY_ID" "$DATASET_ID" "$CONCEPT_PATH"
