# to be included from <schema>/Makefile

BASE_FOLDER = ../../..
include $(BASE_FOLDER)/makefile.inc
include $(BASE_FOLDER)/lib/makefile.inc
OUTDIR := ../../common/$(THIS_SCHEMA)
OVERDIR := ../override/$(THIS_SCHEMA)

# these env variables are used by LoadTsvFile.groovy
NLS_DATE_FORMAT := yyyy-mm-dd hh24:mi:ss
NLS_TIMESTAMP_FORMAT := $(NLS_DATE_FORMAT).ff
export NLS_DATE_FORMAT
export NLS_TIMESTAMP_FORMAT

dump_%:
	NLS_DATE_FORMAT="$(NLS_DATE_FORMAT)" \
	NLS_TIMESTAMP_FORMAT="$(NLS_TIMESTAMP_FORMAT)" \
	groovy -cp '$(CP_ORA):$(BASE_FOLDER)/samples/oracle/' $(BASE_FOLDER)/samples/oracle/DumpTableData.groovy \
	  -t $(THIS_SCHEMA).$* \
	  -o $(OUTDIR)/$*.tsv

# see the commend on override/
ifndef USE_POOL_PROCESS
load_%:
	[ -f $(OVERDIR)/$*.skip ] || \
	  groovy -cp '$(CP_ORA)' $(BASE_FOLDER)/samples/oracle/LoadTsvFile.groovy \
	  -t $(THIS_SCHEMA).$* \
	  -f $$([ -f $(OVERDIR)/$*.tsv ] && echo $(OVERDIR) || echo $(OUTDIR))/$*.tsv
else
load_%: $(NETTY_JAR_PATH) $(JDBC_DRIVER_ORA_PATH)
	[ -f $(OVERDIR)/$*.skip ] || \
	  groovy -cp '$(CP_ORA):$(NETTY_JAR_PATH)' ../submit_tsv_load.groovy \
	  -t $(THIS_SCHEMA).$* \
	  -f $$([ -f $(OVERDIR)/$*.tsv ] && echo $(OVERDIR) || echo $(OUTDIR))/$*.tsv
endif

# vim: set ft=make:
