ifndef KETTLE_JOBS_PSQL
  $(error You need to set KETTLE_JOBS_PSQL; KETTLE_JOBS has been renamed)
endif

include ../../lib/makefile.inc

RDBMS = postgres
JDBC_DRIVER = $(JDBC_DRIVER_PSQL)
KETTLE_JOBS = $(KETTLE_JOBS_PSQL)

$(JDBC_DRIVER): $(JDBC_DRIVER_PSQL_PATH)
	ln -s ../../lib/$(JDBC_DRIVER_PSQL)

include ../common/makefile.inc


.PHONY:  vcf
vcf:
	./_scripts/vcf/load_vcf_data.sh '/tmp/vcf' '$(PSQL_COMMAND) -d $(PGDATABASE)'

export JDBC_DRIVER

