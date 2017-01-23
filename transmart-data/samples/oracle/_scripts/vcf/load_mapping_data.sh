#!/bin/bash

set -e
DIR=`dirname "$0"`
RUN_SQL_DIR=$DIR/../../../../ddl/oracle/_scripts
RUN_SQL_COMMAND="groovy -cp $LIB_CLASSPATH:$RUN_SQL_DIR $RUN_SQL_DIR/run_sql.groovy"


# Check input parameters
if [ $# -lt 1 ]
  then
    echo "No or invalid arguments supplied."

    echo "Usage: ./load_mapping_data.sh vcf_data_dir"
    echo "    vcf_data_dir is the directory containing the parsed data from the VCF and mapping files"
    echo ""
    echo "Example: ./load_mapping_data.sh /tmp/vcf"

    exit 1
fi

output_dir=$1

# List of SQL files to be loaded
SQLFILES=( "load_concept_dimension" "load_observation_fact" \
        "load_i2b2" "load_i2b2_secure" "load_de_subject_sample_mapping" )

# Loop through the SQL files
file_params=
for SQLFILE in "${SQLFILES[@]}"
do
    echo "Processing SQL file $SQLFILE"

    # Converting sql from postgres to oracle
    # Not needed anymore, as SQL is written specifically for oracle
    # sed "s/\(curr\|next\)val( '\([^']*\)' )/\2.\1val/g" "$output_dir/$SQLFILE.sql" > "$output_dir/$SQLFILE.oracle.sql"

    file_params=" -f \"$output_dir/$SQLFILE.sql\" $file_params";
done

$RUN_SQL_COMMAND -t $file_params

# Execute stored procedure to update concept counts
echo "call tm_cz.i2b2_create_concept_counts('\\$CONCEPT_PATH\\');" | $RUN_SQL_COMMAND
