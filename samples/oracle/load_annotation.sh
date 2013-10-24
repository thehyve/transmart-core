#!/bin/bash

set -x
set -e

# load definitions in annotations.params
source $1

make $JDBC_DRIVER

groovy -cp $JDBC_DRIVER InsertGplInfo.groovy \
	-p "$PLATFORM" \
	-t "$TITLE" \
	-o "$ORGANISM" || { test $? -eq 3 && exit 0; }
# the exit code is 3 if we are skip the rest
# due to annotation being already loaden


groovy -cp $JDBC_DRIVER LoadTsvFile.groovy \
	-t tm_lz.lt_src_deapp_annot \
	-c gpl_id,probe_id,gene_symbol,gene_id,organism \
	-f $DATA_LOCATION/$ANNOTATIONS_FILE \
	--truncate

groovy -cp $JDBC_DRIVER RunStoredProcedure.groovy
