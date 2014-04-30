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

$(TRANSMART_LOADER)/conf/log4j.properties: $(TRANSMART_LOADER)/conf/log4j.properties.sample
	cp $< $@

BIOMART_PSW ?= biomart
DEAPP_PSW ?= deapp
SEARCHAPP_PSW ?= searchapp
$(TRANSMART_LOADER)/conf/Common.properties:
	echo "driver_class=org.postgresql.Driver" > $@ \
	&& echo "url=jdbc:postgresql://localhost:$(PGPORT)/$(PGDATABASE)" >> $@ \
	&& echo "biomart_username=biomart" >> $@ \
	&& echo "biomart_password=$(BIOMART_PSW)" >> $@ \
	&& echo "deapp_username=deapp" >> $@ \
	&& echo "deapp_password=$(DEAPP_PSW)" >> $@ \
	&& echo "searchapp_username=searchapp" >> $@ \
	&& echo "searchapp_password=$(SEARCHAPP_PSW)" >> $@

config-transmart-loader: $(TRANSMART_LOADER)/conf/log4j.properties $(TRANSMART_LOADER)/conf/Common.properties

load_proteomics_dictionary: config-transmart-loader

.PHONY: row_counts analyze $(TRANSMART_LOADER)/conf/Common.properties

# vim: set filetype=make:
