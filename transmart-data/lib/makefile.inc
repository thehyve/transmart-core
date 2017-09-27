JDBC_DL_PSQL     := https://repo.thehyve.nl/service/local/repositories/central/content/org/postgresql/postgresql/9.4.1211.jre7
JDBC_DRIVER_PSQL := postgresql-9.4.1211.jre7.jar
JDBC_DL_ORA      := https://repo.thehyve.nl/service/local/repositories/thirdparty/content/com/oracle/ojdbc7/12.1.0.1
JDBC_DRIVER_ORA  := ojdbc7-12.1.0.1.jar
JDBC_DL_XDB_ORA  := http://files.thehyve.net
JDBC_XDB_ORA     := xdb.jar

LIB_DIR          := $(dir $(lastword $(MAKEFILE_LIST)))
CP_ORA           := $(LIB_DIR)$(JDBC_DRIVER_ORA):$(LIB_DIR)$(JDBC_XDB_ORA):$(LIB_DIR)
CP_PSQL          := $(LIB_DIR)$(JDBC_DRIVER_PSQL):$(LIB_DIR)

JDBC_DRIVER_PSQL_PATH := $(LIB_DIR)$(JDBC_DRIVER_PSQL)
JDBC_DRIVER_ORA_PATH  := $(LIB_DIR)$(JDBC_DRIVER_ORA)
JDBC_XDB_ORA_PATH     := $(LIB_DIR)$(JDBC_XDB_ORA)

$(JDBC_DRIVER_PSQL_PATH):
	curl -f $(JDBC_DL_PSQL)/$(JDBC_DRIVER_PSQL) > $@

# For correctness, the dependency on XDB should be split off since some uses of this rule don't need XCB, but it doesn't matter a lot.
$(JDBC_DRIVER_ORA_PATH): $(JDBC_XDB_ORA_PATH)
	curl -f $(JDBC_DL_ORA)/$(JDBC_DRIVER_ORA) > $@

$(JDBC_XDB_ORA_PATH):
	curl -f $(JDBC_DL_XDB_ORA)/$(JDBC_XDB_ORA) > $@

NETTY_JAR        := netty-3.5.13.Final.jar
NETTY_JAR_DL     := http://files.thehyve.net
NETTY_JAR_PATH   := $(LIB_DIR)$(NETTY_JAR)

$(NETTY_JAR_PATH):
	curl -f $(NETTY_JAR_DL)/$(NETTY_JAR) > $@

SCHEMA_SPY_VERSION=5.0.0
SCHEMA_SPY_JAR=schemaSpy_$(SCHEMA_SPY_VERSION).jar
SCHEMA_SPY_JAR_PATH  := $(LIB_DIR)$(SCHEMA_SPY_JAR)
$(SCHEMA_SPY_JAR_PATH):
	curl -L -f "https://downloads.sourceforge.net/project/schemaspy/schemaspy/SchemaSpy%20$(SCHEMA_SPY_VERSION)/$(SCHEMA_SPY_JAR)" > $@
	
.DELETE_ON_ERROR:

# vim: ft=make list
