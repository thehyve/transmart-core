JDBC_DL_PSQL     := http://files.thehyve.net
JDBC_DRIVER_PSQL := postgresql-9.2-1003.jdbc4.jar
JDBC_DL_ORA      := http://files.thehyve.net
JDBC_DRIVER_ORA  := ojdbc6-12.1.0.2.jar
JDBC_XDB_ORA     := xdb.jar

LIB_DIR          := $(dir $(lastword $(MAKEFILE_LIST)))
CP_ORA           := $(LIB_DIR)$(JDBC_DRIVER_ORA):$(LIB_DIR)$(JDBC_XDB_ORA):$(LIB_DIR)
CP_PSQL          := $(LIB_DIR)$(JDBC_DRIVER_PSQL):$(LIB_DIR)

JDBC_DRIVER_PSQL_PATH := $(LIB_DIR)$(JDBC_DRIVER_PSQL)
JDBC_DRIVER_ORA_PATH  := $(LIB_DIR)$(JDBC_DRIVER_ORA)
JDBC_XDB_ORA_PATH     := $(LIB_DIR)$(JDBC_XDB_ORA)

$(JDBC_DRIVER_PSQL_PATH):
	curl -f $(JDBC_DL_PSQL)/$(JDBC_DRIVER_PSQL) > $@

$(JDBC_DRIVER_ORA_PATH): $(JDBC_XDB_ORA_PATH)
	curl -f $(JDBC_DL_ORA)/$(JDBC_DRIVER_ORA) > $@

$(JDBC_XDB_ORA_PATH):
	curl -f $(JDBC_DL_ORA)/$(JDBC_XDB_ORA) > $@

NETTY_JAR        := netty-3.5.13.Final.jar
NETTY_JAR_DL     := http://files.thehyve.net
NETTY_JAR_PATH   := $(LIB_DIR)$(NETTY_JAR)

$(NETTY_JAR_PATH):
	curl -f $(NETTY_JAR_DL)/$(NETTY_JAR) > $@

.DELETE_ON_ERROR:

# vim: ft=make list
