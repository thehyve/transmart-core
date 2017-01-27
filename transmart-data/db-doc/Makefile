include ../lib/makefile.inc

generate_postgresql: $(SCHEMA_SPY_JAR_PATH) $(JDBC_DRIVER_PSQL_PATH)
	java -jar $(SCHEMA_SPY_JAR_PATH) -t pgsql -dp $(JDBC_DRIVER_PSQL_PATH) -db $(PGDATABASE) -host $(if $(PGHOST),$(if $(findstring "",$(PGHOST)),localhost,$(PGHOST)),localhost)$(if $(PGPORT),:$(PGPORT),) -u $(PGUSER) $(if $(PGPASSWORD),-p $(PGPASSWORD),) -o postgres -all

generate_oracle: $(SCHEMA_SPY_JAR_PATH) $(JDBC_DRIVER_ORA_PATH)
	java -jar $(SCHEMA_SPY_JAR_PATH) -t ora -dp $(JDBC_DRIVER_ORA_PATH) -db $(ORASID) -host $(ORAHOST) -u $(ORAUSER) $(if $(ORAPASSWORD),-p $(ORAPASSWORD),) -o oracle -all