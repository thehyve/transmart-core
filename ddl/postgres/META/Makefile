include ../../../makefile.inc

ALL_FIXES = fix_tablespaces fix_owners fix_permissions

all_meta_fixes: $(ALL_FIXES)
.PHONY: all_meta_fixes

.PHONY: fix_%
fix_%: assign_%.sql
	$(PSQL_COMMAND) -f $<

$(ALL_FIXES): fix_support

default_permissions.tsv: default_permissions.php
	php $< > $@

fix_support: biomart_user_write.tsv default_permissions.tsv

clean:
	rm -f default_permissions.tsv

.PHONY: clean

SCHEMAS = tm_cz \
		  tm_wz \
		  tm_lz \
		  i2b2metadata \
		  i2b2demodata \
		  searchapp \
		  deapp \
		  biomart

$(SCHEMAS) :
	mkdir $@

biomart/table.sql : biomart
	$(PGDUMP_COMMAND) -x -s -O --no-tablespaces -n $< > $@
