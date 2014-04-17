include ../../makefile.inc
include ../../lib/makefile.inc

OUTDIR := ../common/$(THIS_SCHEMA)
DBMS_DATA_FOLDER := $(notdir $(shell pwd))

dump_%:
	groovy -cp $(DIR)*:../../samples/oracle ../../samples/oracle/DumpTableData.groovy \
        -t $(THIS_SCHEMA).$* \
        -o $(OUTDIR)/$*.tsv

load_%:
	groovy -cp $(DIR)*:../../samples/oracle ../../samples/oracle/LoadTsvFile.groovy \
        -t $(THIS_SCHEMA).$* \
        -f $(OUTDIR)/$*.tsv

%:
	DBMS_DATA_FOLDER=$(DBMS_DATA_FOLDER) $(MAKE) -C ../common $*

# vim: set filetype=make:
