include ../../../lib/makefile.inc

FILES = \
	../../../ddl/oracle/i2b2demodata/relation_type
	../../../ddl/oracle/i2b2demodata/relation
	data_migration
	../common/data_migration


RUN_SQL   = groovy -cp '$(CP_ORA)' ../../../ddl/oracle/_scripts/run_sql.groovy

migrate: $(JDBC_DRIVER_ORA_PATH)
	$(RUN_SQL) --sequential $(foreach u,$(FILES),-f $(u).sql)
