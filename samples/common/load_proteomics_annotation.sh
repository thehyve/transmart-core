#!/bin/bash

#set -x
set -e

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="annotation"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"

# Execute some basic checks
if [ -z $GPL_ID ] ; then
    echo "Error $0: No value provided for parameter GPL_ID"
    exit 1
fi

if [ ! -d logs ] ; then mkdir logs; fi

$KITCHEN -norep=Y						\
-file="$KETTLE_JOBS/load_proteomics_annotation.kjb"		\
-log="logs/load_proteomics_annotation_$(date +"%Y%m%d%H%M").log" \
-param:DATA_LOCATION="$DATA_LOCATION"				\
-param:SORT_DIR=/tmp						\
-param:GPL_ID="$GPL_ID"						\
-param:LOAD_TYPE=I						\
-param:ANNOTATION_TITLE="$ANNOTATION_TITLE"
