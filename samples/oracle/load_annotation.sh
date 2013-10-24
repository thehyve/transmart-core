#!/bin/bash

groovy -cp $JDBC_DRIVER InsertGplInfo.groovy -p $PLATFORM -t $TITLE -o $ORGANISM

groovy -cp $JDBC_DRIVER LoadTsvFile.groovy -f $DATA_LOCATION/$ANNOTATIONS_FILE -t tm_lz.lt_src_deapp_annot

groovy -cp $JDBC_DRIVER RunStoredProcedure.groovy
