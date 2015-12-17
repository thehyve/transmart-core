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

$(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties: Common.properties.php
	php -d variables_order=E $< > $@

.PHONY: row_counts analyze $(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties

# vim: set filetype=make:
