#!/bin/bash

set -e
# Check input parameters
if [ $# -lt 2 ]
  then
    echo "No or invalid arguments supplied."
    
    echo "Usage: ./load_mapping_data.sh vcf_data_dir"
    echo "    vcf_data_dir is the directory containing the parsed data from the VCF and mapping files"
    echo ""
    echo "Example: ./load_mapping_data.sh /tmp/vcf psql_command"

    exit 1
fi

output_dir=$1
PSQL_COMMAND=$2

# List of SQL files to be loaded
SQLFILES=( "load_concept_dimension" "load_observation_fact" \
        "load_i2b2" "load_i2b2_secure" \
	    "load_de_subject_sample_mapping" )

# Loop through the SQL files
for SQLFILE in "${SQLFILES[@]}"
do
    echo "Processing and converting SQL file $SQLFILE"

    # Replace calls to sequences in the SQL files
    # Replace calls to nextval and currval
    perl -pe "s/([a-zA-Z0-9\._]*)\.(curr|next)val/\2val( '\1' )/g; s/from dual//g" < "$output_dir/$SQLFILE.sql" > "$output_dir/$SQLFILE.postgres.sql"

    $PSQL_COMMAND -f "$output_dir/$SQLFILE.postgres.sql";
done

# Execute stored procedure to update concept counts
$PSQL_COMMAND -c "select tm_cz.i2b2_create_concept_counts('\\$CONCEPT_PATH\\');"
