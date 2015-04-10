#!/bin/bash

set -e

# load definitions in annotations.params
source $1

ALREADY_LOADED=`$PSQL_COMMAND -c "select exists \
		 (select platform from deapp.de_gpl_info where platform = '$PLATFORM')" -tA`
if [ $ALREADY_LOADED = 't' ]; then
	echo -e "\e[33mWARNING\e[m: Platform $PLATFORM already loaded; skipping" >&2
	exit 0
else

$PSQL_COMMAND -c "COPY deapp.de_gpl_info(platform, title, organism, marker_type) FROM STDIN" <<HEREDOC
$PLATFORM	$TITLE	$ORGANISM	Gene Expression
HEREDOC
fi

$PSQL_COMMAND -c "TRUNCATE tm_lz.lt_src_deapp_annot"
$PSQL_COMMAND -c "COPY tm_lz.lt_src_deapp_annot FROM STDIN CSV DELIMITER E'\\t'" < \
		$DATA_LOCATION/$ANNOTATIONS_FILE

RESULT=`$PSQL_COMMAND -c "SELECT TM_CZ.i2b2_load_annotation_deapp()" -tA`
#i2b2_load_annotation_deapp() returns 1 on success
if [ $RESULT -ne 1  ]; then
	echo -e "\e[31mERROR\e[m: Call to load function failed; check error/audit tables" >&2
	exit 1
fi
