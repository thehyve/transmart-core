#!/bin/bash

# This script is used to load VCF data for a specific study
# The script requires 1 parameter: the params file to be used
# If the parameter is not given, the user is expected to set
# the parameters from the params file himself.

#set -x
set -e 

DIR=`dirname $0`

if [ $# -eq 0 ]; then
	# Not called from makefile 
	UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
	UPLOAD_DATA_TYPE="vcf"
	source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"
	if [ -z "$DATA_LOCATION" ] || [ -z "$VCF_FILE" ]   || [ -z "$VCF_TEMP_DIR" ] || \
	   [ -z "$DATASOURCE" ]    || [ -z "$DATASET_ID" ] || [ -z "$GENOME_BUILD" ] || [ -z "$ETL_USER" ]; then
		echo "The following environmental variables should be defined:"
		echo "    DATA_LOCATION=$DATA_LOCATION"
		echo "    VCF_FILE=$VCF_FILE"
		echo "    VCF_TEMP_DIR=$VCF_TEMP_DIR"
		echo "    DATASOURCE=$DATASOURCE"
		echo "    DATASET_ID=$DATASET_ID"
		echo "    GENOME_BUILD=$GENOME_BUILD"
		echo "    ETL_USER=$ETL_USER"
		exit -1
	fi
	export DATA_LOCATION VCF_FILE VCF_TMP_DIR DATASOURCE DATASET_ID GENOME_BUILD ETL_USER
	export PSQL=$PGSQL_BIN/psql
	$DIR/../common/_scripts/vcf/parse_vcf.sh

else
	# called from makefile
	source $1
fi


$DIR/_scripts/vcf/load_vcf_data.sh "$VCF_TEMP_DIR" "$PSQL"
$DIR/_scripts/vcf/load_mapping_data.sh "$VCF_TEMP_DIR" "$PSQL"
