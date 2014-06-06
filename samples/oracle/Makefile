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

.PHONY:  load_vcf load_vcf_data load_vcf_mapping
load_vcf: load_vcf_data load_vcf_mapping
load_vcf_data: $(JDBC_DRIVER)
	./_scripts/vcf/load_vcf_data.sh "$(VCF_TEMP_DIR)"

load_vcf_mapping: $(JDBC_DRIVER)
	./_scripts/vcf/load_mapping_data.sh "$(VCF_TEMP_DIR)"

export JDBC_DRIVER
