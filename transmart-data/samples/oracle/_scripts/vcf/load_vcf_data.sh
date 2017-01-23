#!/bin/bash

set -e
DIR=`dirname "$0"`
RUN_SQL_DIR=$DIR/../../../../ddl/oracle/_scripts
RUN_SQL_COMMAND="groovy -cp $LIB_CLASSPATH:$RUN_SQL_DIR $RUN_SQL_DIR/run_sql.groovy"
LOAD_TSV_COMMAND="groovy -cp $LIB_CLASSPATH LoadTsvFile.groovy"
LOAD_PLATFORM_COMMAND="groovy -cp $LIB_CLASSPATH InsertGplInfo.groovy"


# Check input parameters
if [ $# -lt 1 ]
  then
    echo "No or invalid arguments supplied."

    echo "Usage: ./load_vcf_data.sh vcf_data_dir"
    echo "    vcf_data_dir is the directory containing the parsed data from the VCF and mapping files"
    echo ""
    echo "Example: ./load_vcf_data.sh /tmp/vcf"

    exit 1
fi

output_dir=$1

# First load the dataset
source $output_dir/load_platform.params
$LOAD_PLATFORM_COMMAND \
	-p "$PLATFORM" \
	-t "$PLATFORM_TITLE" \
	-m "$MARKER_TYPE" \
	-g "$GENOME_BUILD" \
	-o "$ORGANISM"

# List of TSV files to be loaded
TSVFILES=( "load_variant_dataset" "load_variant_metadata" "load_variant_subject_idx" \
	    "load_variant_subject_summary" 	"load_variant_subject_detail" \
	    "load_variant_population_info" 	"load_variant_population_data" )

# Loop through the TSV file descriptors
for TSVFILE in "${TSVFILES[@]}"
do
    # source the control file, so we know what file
    # to load and what parameters to use
    source "$DIR/../../../common/_scripts/vcf/$TSVFILE.ctl"

    echo "Processing text file $FILENAME"
		# Tab delimiter must be escaped when calling groovy script
    if [ $DELIMITER = "\t" ]
    then
		$LOAD_TSV_COMMAND \
			-t $TABLE \
			-c "$COLUMNS" \
			-d $'\t' \
			-n "\\N" \
			-f "$output_dir/$FILENAME"
    else
		$LOAD_TSV_COMMAND \
			-t $TABLE \
			-c "$COLUMNS" \
			-d "$DELIMITER" \
			-n "\\N" \
			-f "$output_dir/$FILENAME"
    fi

done
