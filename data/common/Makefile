include ../../makefile.inc

#leave kept lines in original order and add new lines at the end
#we need a temporary file because we want to read and write from
#the same file

minimize_diffs:
	/bin/bash -c '                                                       \
	for f in `php find_chg_tsv.php`; do                                  \
		exec 3< <(git show :./$$f);                                      \
		read -a stat_bef < <(git diff --numstat -- $$f);                 \
		temp_file=temp.$$RANDOM;                                         \
		cp $$f $$temp_file;                                              \
		echo minimize_diff for $$f >&2;                                  \
		php -d memory_limit=1500m minimize_diff.php < $$temp_file > $$f; \
		rm $$temp_file;                                                  \
		exec 3>&-;                                                       \
		read -a stat_aft < <(git diff --numstat -- $$f);                 \
		echo diff: +$${stat_bef[0]}/-$${stat_bef[1]} -">"                \
			+$${stat_aft[0]:-0}/-$${stat_aft[1]:-0} >&2;                 \
	done'

.PHONY: minimize_diffs

TABLE_FILES = $(wildcard *_list)
TABLE_DIRS = $(patsubst %_list,%,$(TABLE_FILES))
DUMP_TARGETS = $(foreach schema,$(notdir $(TABLE_DIRS)),dump_schema_$(schema))
LOAD_TARGETS = $(foreach schema,$(notdir $(TABLE_DIRS)),load_schema_$(schema))

# for autocompletion
$(DUMP_TARGETS) $(LOAD_TARGETS):

dump: $(DUMP_TARGETS)

load: $(LOAD_TARGETS)

$(TABLE_DIRS):
	mkdir $@

%/Makefile: $(TABLE_DIRS)
	php gen_makefile.php $* > $*/Makefile

ifndef DBMS_DATA_FOLDER
        $(error DBMS_DATA_FOLDER variable is not set)
endif

dump_schema_%: %/Makefile
	DBMS_DATA_FOLDER=$(DBMS_DATA_FOLDER) $(MAKE) -C $* dump
load_schema_%: %/Makefile
	DBMS_DATA_FOLDER=$(DBMS_DATA_FOLDER) $(MAKE) -C $* load
.PHONY: dump_schema_% load_schema_%
.PRECIOUS: $(foreach d,$(TABLE_DIRS),$d)

clean:
	rm -f $(foreach d,$(TABLE_DIRS),$d/Makefile); \
	$(foreach d,$(TABLE_DIRS),[ -d $d ] && rmdir $d;) \
	true

#don't include this in clean; dumping everything should only be needed sporadically
clean_dumps:
	rm -rf */*.tsv

ifeq ($(THIS_SCHEMA), searchapp)
	#special case
	load_plugin_module: load_plugin update_plugin_module
load: load_plugin_module

update_plugin_module:
	$(MAKE) -C searchapp/plugin_modules_params searchapp/plugin_module.tsv

.PHONY: update_plugin_module
endif

# vim: set filetype=make:
