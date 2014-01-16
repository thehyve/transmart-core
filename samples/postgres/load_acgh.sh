#!/bin/bash

set -x
set -e

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

if [ $REGION_PLATFORM_FILE ]; then
	AWK_RESULT=`awk -F'\t' 'NR == 2 { print $1"$"$10 }' $DATA_LOCATION/$REGION_PLATFORM_FILE`
	PLATFORM_ID=`echo $AWK_RESULT | cut -d$ -f1`
	ORGANISM=`echo $AWK_RESULT | cut -d$ -f2`
	if [ $PLATFORM_ID ]; then
		$PSQL_COMMAND <<_END
delete from deapp.de_chromosomal_region where gpl_id = '$PLATFORM_ID';
delete from deapp.de_gpl_info where platform = '$PLATFORM_ID';
insert into deapp.de_gpl_info(PLATFORM, TITLE, ORGANISM, MARKER_TYPE) VALUES ('$PLATFORM_ID', '$PLATFORM_ID', '$ORGANISM', 'Chromosomal');
\copy deapp.de_chromosomal_region (GPL_ID, REGION_NAME, CHROMOSOME, START_BP, END_BP, NUM_PROBES, CYTOBAND, GENE_SYMBOL, GENE_ID, ORGANISM) from '$DATA_LOCATION/$REGION_PLATFORM_FILE' with (FORMAT csv, DELIMITER E'\t', NULL '', HEADER)
_END
	else
		echo -e "Can't get platform id from "$PLATFORM_ID" file." >&2
	fi
else
	echo "No platform file specified"
fi

TEMPDIR=$(mktemp -d -t load_acgh_XXXXXX)
trap 'rm -rf $TEMPDIR' EXIT

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
-param:SECURITY_REQUIRED=N                                         \
-param:SORT_DIR=$TEMPDIR                                           \
-param:SOURCE_CD=STD                                               \
-param:STUDY_ID=$STUDY_ID                                          \
"-param:TOP_NODE=$TOP_NODE"

#-param:SQLLDR_PATH=/spin/pg/master/bin/psql \
