JDBC_DL_PSQL = http://files.thehyve.net
JDBC_DRIVER_PSQL = postgresql-9.2-1003.jdbc4.jar
JDBC_DL_ORA = http://files.thehyve.net
JDBC_DRIVER_ORA = ojdbc6-12.1.0.1.0.jar

DIR = $(dir $(lastword $(MAKEFILE_LIST)))

JDBC_DRIVER_PSQL_PATH = $(DIR)$(JDBC_DRIVER_PSQL)
JDBC_DRIVER_ORA_PATH = $(DIR)$(JDBC_DRIVER_ORA)

$(JDBC_DRIVER_PSQL_PATH):
	curl -f $(JDBC_DL_PSQL)/$(JDBC_DRIVER_PSQL) > $@

$(JDBC_DRIVER_ORA_PATH):
	curl -f $(JDBC_DL_ORA)/$(JDBC_DRIVER_ORA) > $@

.DELETE_ON_ERROR:

# vim: ft=make list
