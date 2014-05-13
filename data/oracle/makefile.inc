# to be included from <schema>/Makefile

BASE_FOLDER = ../../..
include $(BASE_FOLDER)/makefile.inc
include $(BASE_FOLDER)/lib/makefile.inc
OUTDIR := ../../common/$(THIS_SCHEMA)

NLS_DATE_FORMAT := yyyy-mm-dd hh24:mi:ss
NLS_TIMESTAMP_FORMAT := $(NLS_DATE_FORMAT).ff

dump_%:
	NLS_DATE_FORMAT="$(NLS_DATE_FORMAT)" \
	NLS_TIMESTAMP_FORMAT="$(NLS_TIMESTAMP_FORMAT)" \
	groovy -cp $(BASE_FOLDER)/lib/*:$(BASE_FOLDER)/samples/oracle $(BASE_FOLDER)/samples/oracle/DumpTableData.groovy \
	-t $(THIS_SCHEMA).$* \
	-o $(OUTDIR)/$*.tsv

load_%:
	NLS_DATE_FORMAT="$(NLS_DATE_FORMAT)" \
	NLS_TIMESTAMP_FORMAT="$(NLS_TIMESTAMP_FORMAT)" \
	groovy -cp $(BASE_FOLDER)/lib/*:$(BASE_FOLDER)/samples/oracle $(BASE_FOLDER)/samples/oracle/LoadTsvFile.groovy \
	-t $(THIS_SCHEMA).$* \
	-f $(OUTDIR)/$*.tsv
