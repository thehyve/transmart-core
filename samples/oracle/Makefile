ifndef KETTLE_JOBS_ORA
  $(error You need to set KETTLE_JOBS_ORA)
endif

include ../../lib/makefile.inc

RDBMS = oracle
JDBC_DRIVER = $(JDBC_DRIVER_ORA)
KETTLE_JOBS = $(KETTLE_JOBS_ORA)

$(JDBC_DRIVER): $(JDBC_DRIVER_ORA_PATH)
	ln -s ../../lib/$(JDBC_DRIVER_ORA)

include ../common/makefile.inc

export JDBC_DRIVER
