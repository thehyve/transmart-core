include ../../makefile.inc

SCHEMAS = \
		  i2b2metadata \
		  i2b2demodata \
		  biomart \
		  deapp \
		  tm_lz \
		  tm_cz \
		  tm_wz \
		  searchapp

FULL_SCHEMA_DUMPS = $(foreach schema, $(SCHEMAS), full_$(schema).sql)

SCHEMA_SQLFILES = $(foreach schema, $(SCHEMAS), $(schema)_files)

SCHEMA_FUNCTIONS = $(foreach schema, $(SCHEMAS), $(schema)/functions)

MACROED_FUNCTION_FILES=$(patsubst macroed_functions/%.pre.sql, \
					   tm_cz/functions/%.sql, $(wildcard macroed_functions/*.pre.sql))

all: $(foreach schema, $(SCHEMAS), $(schema)_files)

files_all: $(SCHEMA_SQLFILES)

$(SCHEMAS) $(SCHEMA_FUNCTIONS):
	mkdir -p $@

$(FULL_SCHEMA_DUMPS):
	$(PGDUMP_COMMAND) -x -s -O --no-tablespaces -n $(subst full_,,$(basename $@)) > $@

%_files: full_%.sql % %/functions _scripts/split_schema.php _scripts/classes.php
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
	rm -f $(FULL_SCHEMA_DUMPS)

.PHONY: clean clean_macroed_functions all files_all

# Loading data
LOAD_TARGETS = $(foreach schema, $(SCHEMAS), load_$(schema))

load_global:
	test $$skip_ddl_global = "1" || $(MAKE) -C GLOBAL load

$(LOAD_TARGETS): load_global

#dependency just to get an error when nothing's available
load_%: %/_load_all.sql
	test `$(PSQL_COMMAND) -c "select exists(select * from pg_namespace where nspname = '$(subst load_,,$@)')" -tA` = t || \
		$(PSQL_COMMAND) -f $<

#dependencies between schemas
load_searchapp: load_biomart
load_tm_cz: load_tm_lz load_i2b2metadata load_i2b2metadata load_searchapp load_biomart
load_tm_lz: load_deapp

load: $(LOAD_TARGETS)
	$(MAKE) -C META load

.PHONY: load load_global
