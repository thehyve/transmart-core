include ../../../lib/makefile.inc

FILES = \
	dimension_description_columns \
    ../common/dimension_description_updates \
    patient_set_collection_index

RUN_SQL   = groovy -cp '$(CP_ORA)' ../../../ddl/oracle/_scripts/run_sql.groovy

migrate: $(JDBC_DRIVER_ORA_PATH)
	$(RUN_SQL) --sequential $(foreach u,$(FILES),-f $(u).sql)
