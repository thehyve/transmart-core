include ../../../makefile.inc

ALL_FIXES = fix_tablespaces fix_owners fix_permissions

ifeq ($(PGVERSION), 11)
    FIX_VERSION := 11
else ifeq ($(PGVERSION), 12)
    FIX_VERSION := 11
else
    FIX_VERSION := default
endif

.PHONY: fix_%
fix_%: assign_%.sql
	test -n "$$skip_fix_$*" || $(PSQL_COMMAND) -f $<

$(ALL_FIXES): fix_support_$(FIX_VERSION)
fix_owners: fix_tablespaces
fix_permissions: fix_owners

default_permissions.tsv: default_permissions.php
	php $< > $@

fix_support_$(FIX_VERSION): biomart_user_write.tsv default_permissions.tsv

clean:
	rm -f default_permissions.tsv

.PHONY: clean

load: $(ALL_FIXES)
.PHONY: load
