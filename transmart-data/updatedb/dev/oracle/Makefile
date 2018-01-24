include ../../../lib/makefile.inc

FILES = \
	../../../ddl/oracle/biomart_user/query
	../../../ddl/oracle/biomart_user/query_set
	../../../ddl/oracle/biomart_user/query_set_instance
	../../../ddl/oracle/biomart_user/query_set_diff
	../../../ddl/oracle/i2b2metadata/i2b2_tag_types
	../../../ddl/oracle/i2b2metadata/i2b2_tag_options
	data_migration
	../common/data_migration


RUN_SQL   = groovy -cp '$(CP_ORA)' ../../../ddl/oracle/_scripts/run_sql.groovy

migrate: $(JDBC_DRIVER_ORA_PATH)
	$(RUN_SQL) --sequential $(foreach u,$(FILES),-f $(u).sql)
