include ../../makefile.inc

.PHONY: fix_%
fix_%: assign_%.sql
	$(PSQL_COMMAND) -f $<

default_permissions.tsv: default_permissions.php
	php $< > $@

fix_support: biomart_user_write.tsv default_permissions.tsv

ALL_FIXES = fix_tablespaces fix_owners fix_permissions

$(ALL_FIXES): fix_support

.PHONY: all_meta_fixes
all_meta_fixes: $(ALL_FIXES)

