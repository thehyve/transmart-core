# to be included from ../{oracle,postgres}/Makefile

SELF_DIR := $(dir $(lastword $(MAKEFILE_LIST)))
include $(SELF_DIR)/../../makefile.inc
COMMON_DIR := $(realpath $(SELF_DIR)/../common)

TABLE_FILES      := $(wildcard $(COMMON_DIR)/*_list)
SCHEMA_DIRS      := $(patsubst %_list,%,$(TABLE_FILES))
SCHEMA_DIRS_DBMS := $(notdir $(SCHEMA_DIRS))
DUMP_TARGETS     := $(foreach schema,$(notdir $(SCHEMA_DIRS)),dump_schema_$(schema))
LOAD_TARGETS     := $(foreach schema,$(notdir $(SCHEMA_DIRS)),load_schema_$(schema))

# for autocompletion
$(DUMP_TARGETS) $(LOAD_TARGETS):

dump: $(DUMP_TARGETS)

load: $(LOAD_TARGETS)

$(foreach schema,$(SCHEMA_DIRS_DBMS),$(schema)/Makefile):

$(COMMON_DIR)/%/makefile.inc:
	$(MAKE) -C $(COMMON_DIR) $*/makefile.inc

.PRECIOUS: $(COMMON_DIR)/%/makefile.inc

%/Makefile: $(COMMON_DIR)/%/makefile.inc
	mkdir -p $*
	printf 'COMMON_DIR := %s\nTHIS_SCHEMA := %s\ninclude ../makefile.inc\n' \
        "$(COMMON_DIR)" $* > $@
	echo 'include ../../common/$*/makefile.inc' >> $@

dump_schema_%: %/Makefile
	$(MAKE) -C $* dump
load_schema_%: %/Makefile
	$(MAKE) -C $* load
.PHONY: dump_schema_% load_schema_%
.PRECIOUS: $(foreach d,$(TABLE_DIRS),$d)


# vim: set filetype=make:
