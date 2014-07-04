ifndef KETTLE_JOBS_PSQL
  $(error You need to set KETTLE_JOBS_PSQL; KETTLE_JOBS has been renamed)
endif

include ../../lib/makefile.inc

RDBMS = postgres
JDBC_DRIVER   := $(JDBC_DRIVER_PSQL_PATH)
LIB_CLASSPATH := $(CP_PSQL)
KETTLE_JOBS   := $(KETTLE_JOBS_PSQL)

include ../common/makefile.inc
