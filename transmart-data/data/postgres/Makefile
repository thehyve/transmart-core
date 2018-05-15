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

.PHONY: row_counts analyze $(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties refresh_mvs

##
# GWAS reference data
##
include ../common/gwas/makefile.inc

SHELL := /bin/bash

load_psql = \
	@if [[ "$$($(PSQL_COMMAND) -c "SELECT EXISTS (SELECT * FROM $(1).$(2) LIMIT 1)" -tA)" = 't' ]]; then \
		echo 'WARNING: The table $(1).$(2) already has data; skipped' >&2; \
	else \
		echo 'Loading $(1).$(2)'; \
		pv -pet '$(3)' | lz4c -d | \
		$(PSQL_COMMAND) -c "COPY $(1).$(2)$(4) FROM STDIN \
			CSV DELIMITER E'\t'"; \
	fi

export PSQL_COMMAND
load_de_rc_snp_info:
	@echo Disabling indexes on de_rc_snp_info
	@sed 's/%ENABLED%/FALSE/;s/%SCHEMA%/deapp/;s/%TABLE%/de_rc_snp_info/' enable_disable_indices.sql | $(PSQL_COMMAND)
	$(call load_psql,deapp,de_rc_snp_info,$(DE_RC_SNP_INFO_FILE),(rs_id, chrom, pos, ref, alt, gene_info, variation_class, strand, hg_version, gene_name, entrez_id))
	@echo Building indexes on de_rc_snp_info
	./rebuild_indexes.sh

load_de_gene_info:
	$(call load_psql,deapp,de_gene_info,$(DE_GENE_INFO_FILE),(entrez_id,gene_symbol,gene_name,chrom,chrom_start,chrom_stop,strand))

load_bio_recombination_rates:
	$(call load_psql,biomart,bio_recombination_rates,$(RECOMBINATION_FILE),)

refresh_mvs:
	$(PSQL_COMMAND) -d $(PGDATABASE) -c "refresh materialized view biomart_user.study_concept_bitset;"

# vim: set filetype=make:
