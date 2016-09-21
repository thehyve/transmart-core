#!/bin/bash

#set -x
set -e

# General optional parameters:
#   DATA_LOCATION, STUDY_NAME, STUDY_ID
# Mandatory parameters specific for this upload script:
#   ANNOTATIONS_FILE
# Optional parameter(s) specific for this upload script:
#   PLATFORM_ID, PLATFORM_TITLE, GENOME_RELEASE

# locate this shell script, and source a generic shell script to process all params related settings
LOAD_SCRIPTS_DIRECTORY=$(pwd)
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="annotation"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"

if [ ! -z "$PLATFORM_DATA_TYPE" ]; then
    case $PLATFORM_DATA_TYPE in
	"expression") echo "expression platform"
		      $LOAD_SCRIPTS_DIRECTORY/load_expression_annotation.sh ${1:+${PARAMS_FILENAME}} ${LOAD_SCRIPTS_DIRECTORY}
		      exit
		      ;;
	"mirna") echo "mirna platform"
		 $LOAD_SCRIPTS_DIRECTORY/load_mirna_annotation.sh ${1:+${PARAMS_FILENAME}}
		 exit
		 ;;
	"mirnaqpcr") echo "mirnaqpcr platform"
		 $LOAD_SCRIPTS_DIRECTORY/load_mirnaqpcr_annotation.sh ${1:+${PARAMS_FILENAME}}
		 exit
		 ;;
	"mirnaseq") echo "mirnaseq platform"
		 $LOAD_SCRIPTS_DIRECTORY/load_mirnaseq_annotation.sh ${1:+${PARAMS_FILENAME}}
		 exit
		 ;;
	"rnaseq") echo "rnaseq platform"
		  $LOAD_SCRIPTS_DIRECTORY/load_rnaseq_annotation.sh ${1:+${PARAMS_FILENAME}}
		  exit
		  ;;
	"rbm") echo "rbm platform"
		 $LOAD_SCRIPTS_DIRECTORY/load_rbm_annotation.sh ${1:+${PARAMS_FILENAME}}
		 exit
	       ;;
	"metabolomics") echo "metabolomics platform"
			$LOAD_SCRIPTS_DIRECTORY/load_metabolomics_annotation.sh ${1:+${PARAMS_FILENAME}}
			exit
			;;
	"msproteomics") echo "msproteomics platform"
		      $LOAD_SCRIPTS_DIRECTORY/load_msproteomics_annotation.sh ${1:+${PARAMS_FILENAME}}
		      exit
		      ;;
	"proteomics") echo "proteomics platform"
		      $LOAD_SCRIPTS_DIRECTORY/load_proteomics_annotation.sh ${1:+${PARAMS_FILENAME}}
		      exit
		      ;;
	"Chromosomal") echo "chromosomal region platform"
		      $LOAD_SCRIPTS_DIRECTORY/load_chromosomal_region_annotation.sh ${1:+${PARAMS_FILENAME}}
		      exit
		      ;;
	*) echo "Unsupported PLATFORM_DATA_TYPE $PLATFORM_DATA_TYPE"
	   exit
	   ;;
    esac
fi


# load definitions in annotations.params
source $1

make "$JDBC_DRIVER"

PLATFORM_TITLE=${PLATFORM_TITLE:-${TITLE:-${PLATFORM}}}
groovy -cp "$LIB_CLASSPATH" InsertGplInfo.groovy \
	-p "$PLATFORM" \
	-t "$PLATFORM_TITLE" \
	-m "Gene Expression" \
	-o "$ORGANISM" || { test $? -eq 3 && exit 0; }
# the exit code is 3 if we are skip the rest
# due to annotation being already loaded


groovy -cp "$LIB_CLASSPATH" LoadTsvFile.groovy \
	-t tm_lz.lt_src_deapp_annot \
	-c gpl_id,probe_id,gene_symbol,gene_id,organism \
	-f $DATA_LOCATION/$ANNOTATIONS_FILE \
	--truncate

groovy -cp "$LIB_CLASSPATH" RunStoredProcedure.groovy
