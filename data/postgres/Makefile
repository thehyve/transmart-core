include ../common/makefile.inc

row_counts:
	$(PSQL_COMMAND) -c "SELECT nspname, relname, reltuples \
		FROM pg_class C \
		INNER JOIN pg_namespace N ON (N.oid = C.relnamespace) \
		WHERE relkind = 'r' \
			AND N.nspname ${SCHEMA_COMP} \
		ORDER BY nspname, reltuples DESC"

analyze:
	$(PSQL_COMMAND) -c 'ANALYZE VERBOSE'

BIOMART_PSW ?= biomart
DEAPP_PSW ?= deapp
SEARCHAPP_PSW ?= searchapp
$(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties:
	echo "driver_class=org.postgresql.Driver" > $@ \
	&& echo "url=jdbc:postgresql://localhost:$(PGPORT)/$(PGDATABASE)" >> $@ \
	&& echo "biomart_username=biomart" >> $@ \
	&& echo "biomart_password=$(BIOMART_PSW)" >> $@ \
	&& echo "deapp_username=deapp" >> $@ \
	&& echo "deapp_password=$(DEAPP_PSW)" >> $@ \
	&& echo "searchapp_username=searchapp" >> $@ \
	&& echo "searchapp_password=$(SEARCHAPP_PSW)" >> $@

load_proteomics_dictionary: $(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties

.PHONY: row_counts analyze $(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties

# vim: set filetype=make:
