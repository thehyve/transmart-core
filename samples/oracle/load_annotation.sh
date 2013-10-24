#!/bin/bash

groovy -cp $JDBC_DRIVER InsertGplInfo.groovy -p $PLATFORM -t $TITLE -o $ORGANISM

groovy -cp $JDBC_DRIVER load_tsv_file.groovy -f some_file -t some_table

groovy -cp $JDBC_DRIVER RunStoredProcedure.groovy
