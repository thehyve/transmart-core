include makefile.inc

postgres:
	$(MAKE) -C ddl/postgres/GLOBAL createdb
	$(MAKE) postgres_load

postgres_load:
	$(MAKE) -C ddl/postgres load
	$(MAKE) -C data/postgres load

postgres_drop:
	$(MAKE) -C ddl/postgres/GLOBAL drop

.PHONY: postgres postgres_load postgres_drop
