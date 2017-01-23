ifndef KETTLE_JOBS_ORA
  $(error You need to set KETTLE_JOBS_ORA)
endif

include ../../lib/makefile.inc

RDBMS = oracle
JDBC_DRIVER   := $(JDBC_DRIVER_ORA_PATH)
LIB_CLASSPATH := $(CP_ORA)
KETTLE_JOBS   := $(KETTLE_JOBS_ORA)
BATCH_PROPS   := batchdb-oracle.properties

include ../common/makefile.inc

$(LOAD_VCF_PARAMS_TARGETS): $(JDBC_DRIVER)

.PHONY:  parse load_vcf load_vcf_data load_vcf_mapping load_parsed_vcf_data
		load_parsed_vcf_mapping

parse_vcf: ../common/parse_vcf
load_vcf: parse_vcf load_parsed_vcf_data load_parsed_vcf_mapping
load_vcf_data: parse_vcf load_parsed_vcf_data
load_vcf_mapping:  parse_vcf load_parsed_vcf_mapping

load_parsed_vcf_data: $(JDBC_DRIVER)
	./_scripts/vcf/load_vcf_data.sh "$(VCF_TEMP_DIR)"

load_parsed_vcf_mapping: $(JDBC_DRIVER)
	./_scripts/vcf/load_mapping_data.sh "$(VCF_TEMP_DIR)"
