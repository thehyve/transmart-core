#!/bin/bash

set -e
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
source $output_dir/load_platform.params
$PSQL_COMMAND -c "insert into deapp.de_gpl_info (platform, title, marker_type, genome_build, organism) \
		select '$PLATFORM', '$PLATFORM_TITLE', '$MARKER_TYPE', '$GENOME_BUILD', '$ORGANISM' \
		WHERE NOT EXISTS(select platform from deapp.de_gpl_info where platform = '$PLATFORM');"

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
			DELIMITER $PGDELIMITER" < $output_dir/$FILENAME;
done
