ifndef KETTLE_JOBS_PSQL
  $(error You need to set KETTLE_JOBS_PSQL; KETTLE_JOBS has been renamed)
endif

include ../../lib/makefile.inc

RDBMS = postgres
JDBC_DRIVER   := $(JDBC_DRIVER_PSQL_PATH)
LIB_CLASSPATH := $(CP_PSQL)
KETTLE_JOBS   := $(KETTLE_JOBS_PSQL)
BATCH_PROPS   := batchdb-psql.properties

include ../common/makefile.inc

.PHONY:  parse load_vcf load_vcf_data load_vcf_mapping load_parsed_vcf_data
		load_parsed_vcf_mapping

parse_vcf: ../common/parse_vcf
load_vcf: ../common/parse_vcf load_parsed_vcf_data load_parsed_vcf_mapping
load_vcf_data: ../common/parse_vcf load_parsed_vcf_data
load_vcf_mapping:  ../common/parse_vcf load_parsed_vcf_mapping

load_parsed_vcf_data:
	./_scripts/vcf/load_vcf_data.sh "$(VCF_TEMP_DIR)" '$(PSQL_COMMAND) -d $(PGDATABASE)'

load_parsed_vcf_mapping:
	./_scripts/vcf/load_mapping_data.sh "$(VCF_TEMP_DIR)" '$(PSQL_COMMAND) -d $(PGDATABASE)'
