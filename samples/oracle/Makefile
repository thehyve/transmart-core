ifndef KETTLE_JOBS_ORA
  $(error You need to set KETTLE_JOBS_ORA)
endif

include ../../lib/makefile.inc

RDBMS = oracle
JDBC_DRIVER   := $(JDBC_DRIVER_ORA_PATH)
LIB_CLASSPATH := $(CP_ORA)
KETTLE_JOBS   := $(KETTLE_JOBS_ORA)

include ../common/makefile.inc
