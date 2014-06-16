#!/bin/bash

# This script is used to parse VCF data for a specific study
# The script requires 1 parameter: the params file to be used
# If the parameter is not given, the user is expected to set
# the parameters from the params file himself.
if [ $# -ge 1 ]
	then
		source $1
fi

DIR=`dirname $0`

# Make sure the output directory exists
mkdir -p "$VCF_TEMP_DIR"

perl $DIR/parseVCFintoTextFiles.pl "$VCF_FILE" "$VCF_TEMP_DIR" "$DATASOURCE" \
		"$DATASET_ID" "$GPL_ID" "$GENOME_BUILD" "$ETL_USER"
perl $DIR/convertMappingIntoSQLFiles.pl "$SUBJECT_SAMPLE_MAPPING_FILE" "$VCF_TEMP_DIR" \
		"$STUDY_ID" "$DATASET_ID" "$CONCEPT_PATH"