# to be included from <schema>/Makefile

include ../../../makefile.inc
OUTDIR := ../../common/$(THIS_SCHEMA)

export PSQL_COMMAND

dump_%:
	$(PSQL_COMMAND) -c "COPY \
			(SELECT $$(../colspec.sh $(THIS_SCHEMA) $*) FROM $(THIS_SCHEMA).$*) \
			TO STDOUT \
			CSV DELIMITER E'\t' FORCE QUOTE *" > $(OUTDIR)/$*.tsv

load_%:
	@if [ `$(PSQL_COMMAND) -c "SELECT EXISTS (SELECT * FROM $(THIS_SCHEMA).$* LIMIT 1)" -tA` = 't' ]; then \
		echo 'WARNING: The table $(THIS_SCHEMA).$* already has data; skipped' >&2; \
	else \
		$(PSQL_COMMAND) -c "COPY $(THIS_SCHEMA).$* FROM STDIN \
			CSV DELIMITER E'\t'" < $(OUTDIR)/$*.tsv; \
	fi

# vim: set ft=make:
