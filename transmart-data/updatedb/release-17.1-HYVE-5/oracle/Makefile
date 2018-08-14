include ../../../lib/makefile.inc

FILES = \
	../common/migrate \
	to_date_data_type

RUN_SQL   = groovy -cp '$(CP_ORA)' ../../../ddl/oracle/_scripts/run_sql.groovy

migrate: $(JDBC_DRIVER_ORA_PATH)
	$(RUN_SQL) --sequential $(foreach u,$(FILES),-f $(u).sql)
