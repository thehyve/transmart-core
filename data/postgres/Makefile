include ../../makefile.inc

TABLE_FILES = $(wildcard *_list)
TABLE_DIRS = $(patsubst %_list,%,$(TABLE_FILES))
DUMP_TARGETS = $(foreach schema,$(TABLE_DIRS),dump_schema_$(schema))
LOAD_TARGETS = $(foreach schema,$(TABLE_DIRS),load_schema_$(schema))

ifdef SCHEMA
	SCHEMA_COMP := = '$(SCHEMA)'
else
	SCHEMA_COMP := IN ('i2b2demodata', 'i2b2metadata', 'deapp', 'biomart', 'searchapp')
endif

row_counts:
	$(PSQL_COMMAND) -c "SELECT nspname, relname, reltuples \
		FROM pg_class C \
		INNER JOIN pg_namespace N ON (N.oid = C.relnamespace) \
		WHERE relkind = 'r' \
			AND N.nspname ${SCHEMA_COMP} \
		ORDER BY nspname, reltuples DESC"

analyze:
	$(PSQL_COMMAND) -c 'ANALYZE VERBOSE'


$(TABLE_DIRS):
	mkdir $@

%/Makefile: %
	php gen_makefile.php $* > $*/Makefile

../common/%:
	mkdir -p $@

.PRECIOUS: $(foreach d,$(TABLE_DIRS),../common/$d)

# for autocompletion
$(DUMP_TARGETS) $(LOAD_TARGETS):

dump_schema_%: %/Makefile ../common/%
	$(MAKE) -C $* dump

load_schema_%: %/Makefile ../common/%
	$(MAKE) -C $* load

dump: $(DUMP_TARGETS)

load: $(LOAD_TARGETS)

.PHONY: dump_schema_% load_schema_%
.PHONY: row_counts analyze dump load

clean:
	rm -f $(foreach d,$(TABLE_DIRS),$d/Makefile); \
		$(foreach d,$(TABLE_DIRS),[ -d $d ] && rmdir $d;) \
		true

#don't include this in clean; dumping everything should only be needed sporadically
clean_dumps:
	rm -rf ../common
