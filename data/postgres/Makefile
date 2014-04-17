include ../../makefile.inc

OUTDIR := ../common/$(THIS_SCHEMA)
DBMS_DATA_FOLDER := $(notdir $(shell pwd))

ifdef THIS_SCHEMA
        SCHEMA_COMP := '$(THIS_SCHEMA)'
else
        SCHEMA_COMP := IN ('i2b2demodata', 'i2b2metadata', 'deapp', 'biomart', 'searchapp')
endif

dump_%:
	$(PSQL_COMMAND) -c "COPY $(THIS_SCHEMA).$* TO STDOUT \
			CSV DELIMITER E'\t' FORCE QUOTE *" > $(OUTDIR)/$*.tsv

load_%:
	@if [ `$(PSQL_COMMAND) -c "SELECT EXISTS (SELECT * FROM $(THIS_SCHEMA).$* LIMIT 1)" -tA` = 't' ]; then \
		echo 'WARNING: The table $(THIS_SCHEMA).$* already has data; skipped' >&2; \
	else \
		$(PSQL_COMMAND) -c "COPY $(THIS_SCHEMA).$* FROM STDIN \
			CSV DELIMITER E'\t'" < $(OUTDIR)/$*.tsv; \
	fi

row_counts:
	$(PSQL_COMMAND) -c "SELECT nspname, relname, reltuples \
		FROM pg_class C \
		INNER JOIN pg_namespace N ON (N.oid = C.relnamespace) \
		WHERE relkind = 'r' \
			AND N.nspname ${SCHEMA_COMP} \
		ORDER BY nspname, reltuples DESC"

analyze:
	$(PSQL_COMMAND) -c 'ANALYZE VERBOSE'

%:
	DBMS_DATA_FOLDER=$(DBMS_DATA_FOLDER) $(MAKE) -C ../common $*

.PHONY: row_counts analyze

# vim: set filetype=make:
