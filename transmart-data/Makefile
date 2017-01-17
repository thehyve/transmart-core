include makefile.inc

postgres:
	$(MAKE) -C ddl/postgres/GLOBAL createdb
	$(MAKE) postgres_load

postgres_load:
	$(MAKE) -C ddl/postgres load
	$(MAKE) -C data/postgres load

postgres_drop:
	$(MAKE) -C ddl/postgres/GLOBAL drop

postgres_test:
	$(MAKE) -C ddl/postgres/GLOBAL createdb
	$(MAKE) -C ddl/postgres load
	$(MAKE) -C data/postgres load_schema_amapp
	$(MAKE) -C test_data/ postgres_test_data

oracle:
	$(MAKE) -C ddl/oracle load
	$(MAKE) -C data/oracle load

oracle_test:
	$(MAKE) -C ddl/oracle load
	$(MAKE) -C data/oracle load_schema_amapp
	$(MAKE) -C test_data/ oracle_test_data

oracle_drop:
	$(MAKE) -C ddl/oracle drop

update_datasets:
	$(MAKE) -C samples/studies update_datasets

.PHONY: postgres postgres_load postgres_drop oracle oracle_drop update_datasets
