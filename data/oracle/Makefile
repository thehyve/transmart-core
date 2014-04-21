include ../../makefile.inc
include ../../lib/makefile.inc

OUTDIR := ../common/$(THIS_SCHEMA)
DBMS_DATA_FOLDER := $(notdir $(shell pwd))
NLS_DATE_FORMAT := yyyy-mm-dd hh24:mi:ss
NLS_TIMESTAMP_FORMAT := $(NLS_DATE_FORMAT).ff

dump_%:
	NLS_DATE_FORMAT="$(NLS_DATE_FORMAT)" \
	NLS_TIMESTAMP_FORMAT="$(NLS_TIMESTAMP_FORMAT)" \
	groovy -cp $(DIR)*:../../samples/oracle ../../samples/oracle/DumpTableData.groovy \
        -t $(THIS_SCHEMA).$* \
        -o $(OUTDIR)/$*.tsv

load_%:
	NLS_DATE_FORMAT="$(NLS_DATE_FORMAT)" \
        NLS_TIMESTAMP_FORMAT="$(NLS_TIMESTAMP_FORMAT)" \
	groovy -cp $(DIR)*:../../samples/oracle ../../samples/oracle/LoadTsvFile.groovy \
        -t $(THIS_SCHEMA).$* \
        -f $(OUTDIR)/$*.tsv

%:
	DBMS_DATA_FOLDER=$(DBMS_DATA_FOLDER) $(MAKE) -C ../common $*

# vim: set filetype=make:
