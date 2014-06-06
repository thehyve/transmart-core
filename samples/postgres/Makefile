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

.PHONY:  load_vcf load_vcf_data load_vcf_mapping
load_vcf: load_vcf_data load_vcf_mapping
load_vcf_data: ../common/parse_vcf
	./_scripts/vcf/load_vcf_data.sh "$(VCF_TEMP_DIR)" '$(PSQL_COMMAND) -d $(PGDATABASE)'

load_vcf_mapping:  ../common/parse_vcf
	./_scripts/vcf/load_mapping_data.sh "$(VCF_TEMP_DIR)" '$(PSQL_COMMAND) -d $(PGDATABASE)'


export JDBC_DRIVER

