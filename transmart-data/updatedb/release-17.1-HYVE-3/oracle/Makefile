include ../../../lib/makefile.inc

FILES = \
	query_blob
	../common/migrate

RUN_SQL   = groovy -cp '$(CP_ORA)' ../../../ddl/oracle/_scripts/run_sql.groovy

migrate: $(JDBC_DRIVER_ORA_PATH)
	$(RUN_SQL) --sequential $(foreach u,$(FILES),-f $(u).sql)
