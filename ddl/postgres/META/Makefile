include ../../../makefile.inc

ALL_FIXES = fix_tablespaces fix_owners fix_permissions

.PHONY: fix_%
fix_%: assign_%.sql
	test -n "$$skip_fix_$*" || $(PSQL_COMMAND) -f $<

$(ALL_FIXES): fix_support
fix_owners: fix_tablespaces
fix_permissions: fix_owners

default_permissions.tsv: default_permissions.php
	php $< > $@

fix_support: biomart_user_write.tsv default_permissions.tsv

clean:
	rm -f default_permissions.tsv

.PHONY: clean

load: $(ALL_FIXES)
.PHONY: load
