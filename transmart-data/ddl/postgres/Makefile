include ../../makefile.inc

SCHEMAS = \
		  i2b2metadata \
		  i2b2demodata \
		  biomart_user \
		  biomart \
		  deapp \
		  tm_lz \
		  tm_cz \
		  tm_wz \
		  searchapp \
		  amapp \
		  fmapp \
		  galaxy \
		  ts_batch

FULL_SCHEMA_DUMPS = $(foreach schema, $(SCHEMAS), _dumps/$(schema).sql)

SCHEMA_DEPENDENCIES = $(foreach schema, $(SCHEMAS), _dumps/$(schema)_deps.tsv)

#phony targets to combine the first two
DUMP_SCHEMA_TARGETS = $(foreach schema, $(SCHEMAS), dump_$(schema))

SCHEMA_SQLFILES = $(foreach schema, $(SCHEMAS), $(schema)_files)

SCHEMA_FUNCTIONS = $(foreach schema, $(SCHEMAS), $(schema)/functions)

MACROED_FUNCTION_FILES=$(patsubst macroed_functions/%.pre.sql, \
					   tm_cz/functions/%.sql, $(wildcard macroed_functions/*.pre.sql))

all: $(foreach schema, $(SCHEMAS), $(schema)_files)

files_all: $(SCHEMA_SQLFILES)

$(SCHEMAS) $(SCHEMA_FUNCTIONS):
	mkdir -p $@

$(FULL_SCHEMA_DUMPS):
	mkdir -p _dumps
	$(PGDUMP_COMMAND) -x -s -O --no-tablespaces -n $(patsubst _dumps/%.sql,%,$@) > $@

$(SCHEMA_DEPENDENCIES):
	$(foreach file,$(wildcard support/*.sql),$(PSQL_COMMAND) -f $(file);)
	mkdir -p _dumps
	$(PSQL_COMMAND) -c "COPY (SELECT * FROM public.views_dependencies WHERE \
			objschema = '$(patsubst _dumps/%_deps.tsv,%,$@)') TO STDOUT \
			CSV DELIMITER E'\t' FORCE QUOTE *" > $@

$(DUMP_SCHEMA_TARGETS):
	$(MAKE) _dumps/$(patsubst dump_%,%,$@).sql _dumps/$(patsubst dump_%,%,$@)_deps.tsv

dump: $(DUMP_SCHEMA_TARGETS)

.PHONY: $(DUMP_SCHEMA_TARGETS) dump

%_files: _dumps/%.sql _dumps/%_deps.tsv _scripts/split_schema.php _scripts/classes.php
	mkdir -p $(patsubst %_files,%/functions,$@)
	php _scripts/split_schema.php $(subst _files,,$@)

#additional dependencies for tm_cz:
tm_cz_files: $(MACROED_FUNCTION_FILES)

$(MACROED_FUNCTION_FILES): tm_cz tm_cz/functions

tm_cz/functions/%.sql: macroed_functions/%.pre.sql _scripts/macros.php
	 php $< > $@

clean_macroed_functions:
	rm -f $(MACROED_FUNCTION_FILES)

#don't remove the rest because we want those generated files to be editable
#manually. Full generation from DB dump be rare.

clean: clean_macroed_functions
	rm -rf _dumps

clean_all: clean
	rm -rf $(SCHEMAS)

.PHONY: clean clean_macroed_functions all files_all clean_all

# Loading data
LOAD_TARGETS = $(foreach schema, $(SCHEMAS), load_$(schema))

load_global:
	test -n "$$skip_ddl_global" || $(MAKE) -C GLOBAL load

load_cross: $(wildcard */_cross.sql)
	#before executing _cross.sql, execute SET statements from prelude
	$(foreach cf,$?, { grep SET $(patsubst %/_cross.sql,%/prelude.sql,$(cf)); \
		cat $(cf); } | $(PSQL_COMMAND);)

$(LOAD_TARGETS): load_global

#dependency just to get an error when nothing's available
load_%: %/_load_all.sql
	test `$(PSQL_COMMAND) -c "select exists(select * from pg_namespace where nspname = '$(subst load_,,$@)')" -tA` = t || \
		$(PSQL_COMMAND) -f $<

load: $(LOAD_TARGETS)
	$(MAKE) load_cross
	$(MAKE) -C META load

.PHONY: load load_global load_cross
