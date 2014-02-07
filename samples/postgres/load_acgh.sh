#!/bin/bash

# This script load the aCGH data for a specific study.
  set -x
  set -e

# Check if environment is set
  if [ -z "$KETTLE_HOME" ]; then
        echo "KETTLE_HOME is not set"
        exit 1
  fi

# $KETTLE_HOME should have been set by the caller
  export KETTLE_HOME

# Should define DATA_FILE_PREFIX, MAP_FILENAME
  source $1

  SECURITY_REQUIRED=${SECURITY_REQUIRED:-N}

  if [ $SECURITY_REQUIRED = 'Y' ]; then
        TOP_NODE_PREFIX='Private Studies'
  else
        TOP_NODE_PREFIX='Public Studies'
  fi

  TOP_NODE='\'$TOP_NODE_PREFIX'\'$STUDY_ID'\'$NODE_NAME


# Get PLATFORM_ID and ORGANISM
  if [ $REGION_PLATFORM_FILE ]; then
	AWK_RESULT=`awk -F'\t' 'NR == 2 { print $1"$"$10 }' $DATA_LOCATION/$REGION_PLATFORM_FILE`
	PLATFORM_ID=`echo $AWK_RESULT | cut -d$ -f1`
	ORGANISM=`echo $AWK_RESULT | cut -d$ -f2`
  fi

# Insert Chromosomal region into the landing-zone
  if [ $PLATFORM_ID ]; then
	echo "Putting Chromosomal region data into tm_lz.lt_chromosomal_region (truncate first)"
	$PSQL_COMMAND << _END
truncate tm_lz.lt_chromosomal_region;
\copy tm_lz.lt_chromosomal_region (GPL_ID, REGION_NAME, CHROMOSOME, START_BP, END_BP, NUM_PROBES, CYTOBAND, GENE_SYMBOL, GENE_ID, ORGANISM) from '$DATA_LOCATION/$REGION_PLATFORM_FILE' with (FORMAT csv, DELIMITER E'\t', NULL '', HEADER);
_END
  fi


# Transfer chromosomal region defintion from landing-zone into deapp-tables
  echo "Transfer chromosomal region data tot the deapp tables"
  $PSQL_COMMAND << _END
        select tm_cz.i2b2_load_chrom_region();
_END

  TEMPDIR=$(mktemp -d -t load_acgh_XXXXXX)
  trap 'rm -rf $TEMPDIR' EXIT


# Opload the chromosomal data
  echo "Uploading the chromosomal data"
  $KITCHEN -norep=Y                                                  \
	-file=$KETTLE_JOBS/load_acgh_data.kjb                              \
	-log='logs/load_'$STUDY_ID'_acgh_data_'$(date +"%Y%m%d%H%M")'.log' \
	-param:DATA_FILE_PREFIX=$DATA_FILE_PREFIX                          \
	-param:DATA_LOCATION=$DATA_LOCATION                                \
	-param:FilePivot_LOCATION=$KETTLE_JOBS'../'                        \
	-param:LOAD_TYPE=I                                                 \
	-param:SAMPLE_REMAP_FILENAME=NOSAMPLEREMAP                         \
	-param:SAMPLE_SUFFIX=.chip                                         \
	-param:MAP_FILENAME=$MAP_FILENAME                                  \
	-param:SECURITY_REQUIRED=$SECURITY_REQUIRED                        \
	-param:SORT_DIR=$TEMPDIR                                           \
	-param:SOURCE_CD=STD                                               \
	-param:STUDY_ID=$STUDY_ID                                          \
	"-param:TOP_NODE=$TOP_NODE"

