ifndef KETTLE_JOBS_PSQL
  $(error You need to set KETTLE_JOBS_PSQL; KETTLE_JOBS has been renamed)
endif
KETTLE_JOBS = $(KETTLE_JOBS_PSQL)
JDBC_DRIVER = postgresql-9.2-1003.jdbc4.jar
JDBC_DL = http://jdbc.postgresql.org/download
RDBMS = postgres

include ../common/makefile.inc

showdblog: $(JDBC_DRIVER)
	groovy -cp $< ../common/dump_audit.groovy $(RDBMS) `tput cols`

.PHONY: showdblog
