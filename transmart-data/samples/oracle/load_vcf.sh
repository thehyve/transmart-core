#!/bin/bash

# This script is used to load VCF data for a specific study
# The script requires 1 parameter: the params file to be used
# If the parameter is not given, the user is expected to set
# the parameters from the params file himself.
if [ $# -ge 1 ]
	then
		source $1
fi

DIR=`dirname $0`

$DIR/_scripts/vcf/load_vcf_data.sh "$VCF_TEMP_DIR"
$DIR/_scripts/vcf/load_mapping_data.sh "$VCF_TEMP_DIR"
