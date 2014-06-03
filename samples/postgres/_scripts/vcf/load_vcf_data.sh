#!/bin/bash

# Check input parameters
if [ $# -lt 2 ]
  then
    echo "No or invalid arguments supplied."
    
    echo "Usage: ./load_vcf_data.sh vcf_data_dir"
    echo "    vcf_data_dir is the directory containing the parsed data from the VCF and mapping files"
    echo ""
    echo "Example: ./load_vcf_data.sh /tmp/vcf psql_command"

    exit 1
fi

output_dir=$1
PSQL_COMMAND=$2
DIR=`dirname "$0"`

# First load the dataset
$PSQL_COMMAND -f "$1/load_platform.sql"

# List of TSV files to be loaded
TSVFILES=( "load_variant_dataset" "load_variant_metadata" "load_variant_subject_idx" \
	    "load_variant_subject_summary" 	"load_variant_subject_detail" \
	    "load_variant_population_info" 	"load_variant_population_data" )

# Loop through the TSV file descriptors
for TSVFILE in "${TSVFILES[@]}"
do
    # source the control file, so we know what file
    # to load and what parameters to use
    . "$DIR/../../../common/_scripts/vcf/$TSVFILE.ctl"

    # Postgres tab delimiter needs a special format
    if [ $DELIMITER = "\t" ]
    then
        PGDELIMITER="E'\t'"
    else
        PGDELIMITER="'$DELIMITER'"
    fi

    echo "Processing text file $FILENAME"
    $PSQL_COMMAND -c "COPY $TABLE ($COLUMNS) FROM STDIN \
			DELIMITER $PGDELIMITER " < $output_dir/$FILENAME;
done

# List of SQL files to be loaded
SQLFILES=( "load_concept_dimension" "load_observation_fact" \
        "load_i2b2" "load_i2b2_secure" \
	    "load_de_subject_sample_mapping" "load_concept_counts" )

# Loop through the SQL files
for SQLFILE in "${SQLFILES[@]}"
do
    echo "Processing SQL file $SQLFILE"
    $PSQL_COMMAND -f "$output_dir/$SQLFILE.sql";
done