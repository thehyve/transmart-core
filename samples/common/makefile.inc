include ../../makefile.inc

# This file is to included from:
#   ../oracle/Makefile
#   ../postgres/Makefile
#
# It still has some PostgreSQL specific stuff; that should be moved into
# PostgreSQL's Makefile and only the common stuff should be here.
# To be done when more analsyes are ported to Oracle

showdblog: $(JDBC_DRIVER)
	groovy -cp $< ../common/dump_audit.groovy $(RDBMS) `tput cols`

.PHONY: showdblog

export KETTLE_JOBS

ifndef KITCHEN
$(error KITCHEN is not set)
endif

CLINICAL_PARAMS_FILES := $(wildcard ../common/*/clinical.params)
ANNOTATION_PARAMS_FILES := $(wildcard ../common/*/annotation.params)
REF_ANNOTATION_PARAMS_FILES := $(wildcard ../common/*/ref_annotation.params)
EXPRESSION_PARAMS_FILES := $(wildcard ../common/*/expression.params)
ANALYSIS_PARAMS_FILES := $(wildcard ../common/*/analysis.params)
ACGH_PARAMS_FILES := $(wildcard ../common/*/acgh.params)
PROTEOMICS_PARAMS_FILES := $(wildcard ../common/*/proteomics.params)
MIRNA_PARAMS_FILES := $(wildcard ../common/*/mirna.params)
VCF_PARAMS_FILES := $(wildcard ../common/*/vcf.params)

LOAD_CLINICAL_TARGETS := $(patsubst ../common/%/clinical.params,load_clinical_%, $(CLINICAL_PARAMS_FILES))
LOAD_ANNOTATION_TARGETS := $(patsubst ../common/%/annotation.params,load_annotation_%, $(ANNOTATION_PARAMS_FILES))
LOAD_REF_ANNOTATION_TARGETS := $(patsubst ../common/%/ref_annotation.params,load_ref_annotation_%, $(REF_ANNOTATION_PARAMS_FILES))
LOAD_EXPRESSION_PARAMS_TARGETS := $(patsubst ../common/%/expression.params,load_expression_%, $(EXPRESSION_PARAMS_FILES))
LOAD_ANALYSIS_PARAMS_TARGETS := $(patsubst ../common/%/analysis.params,load_analysis_%, $(ANALYSIS_PARAMS_FILES))
LOAD_ACGH_PARAMS_TARGETS := $(patsubst ../common/%/acgh.params,load_acgh_%, $(ACGH_PARAMS_FILES))
LOAD_PROTEOMICS_PARAMS_TARGETS := $(patsubst ../common/%/proteomics.params,load_proteomics_%, $(PROTEOMICS_PARAMS_FILES))
LOAD_MIRNA_PARAMS_TARGETS := $(patsubst ../common/%/mirna.params,load_mirna_%, $(MIRNA_PARAMS_FILES))
LOAD_VCF_PARAMS_TARGETS := $(patsubst ../common/%/vcf.params,load_vcf_%, $(VCF_PARAMS_FILES))

KETTLE_TARGETS := $(LOAD_CLINICAL_TARGETS) $(LOAD_EXPRESSION_PARAMS_TARGETS) $(LOAD_ANALYSIS_PARAMS_TARGETS) $(LOAD_ACGH_PARAMS_TARGETS) $(LOAD_PROTEOMICS_PARAMS_TARGETS) $(LOAD_MIRNA_PARAMS_TARGETS)

$(KETTLE_TARGETS): write_kettle_properties
#these ones are here just for autocompletion:
$(LOAD_ANNOTATION_TARGETS):
$(LOAD_REF_ANNOTATION_TARGETS):
$(LOAD_VCF_PARAMS_TARGETS):

write_kettle_properties: gen_kettle_properties.php
	php -d variables_order=E $< > kettle-home/kettle.properties

load_clinical_%: ../common/%/clinical.params ../common/%/clinical
	STUDY_ID=$* DATA_LOCATION="$(realpath ../common/$*/clinical)" \
			KETTLE_HOME="$(realpath kettle-home)" \
			./load_clinical.sh $<

load_annotation_%: ../common/%/annotation.params ../common/%/annotation
	DATA_LOCATION="$(realpath ../common/$*/annotation)" \
		PSQL_COMMAND="$(PSQL_COMMAND)" \
		./load_annotation.sh $<

load_ref_annotation_%: ../common/%/ref_annotation.params
	$(MAKE) load_annotation_`./load_ref_annotation.sh $<`

load_expression_%: ../common/%/expression.params ../common/%/expression
	STUDY_ID=$* DATA_LOCATION="$(realpath ../common/$*/expression)" \
			KETTLE_HOME="$(realpath kettle-home)" \
			./load_expression.sh $<

load_analysis_%: ../common/%/analysis.params ../common/%/analysis
	STUDY_ID=$* DATA_LOCATION="$(realpath ../common/$*/analysis)" \
			KETTLE_HOME="$(realpath kettle-home)" \
			./load_analysis.sh $<

load_acgh_%: ../common/%/acgh.params ../common/%/acgh
	STUDY_ID=$* DATA_LOCATION="$(realpath ../common/$*/acgh)" \
			PSQL_COMMAND="$(PSQL_COMMAND)" \
			KETTLE_HOME="$(realpath kettle-home)" \
			./load_acgh.sh $<

PROTEOMICS_SSM_FILE?=proteomics_subject_sample_mapping.txt
PROTEOMICS_CM_FILE?=proteomics_columns_mapping.txt
PROTEOMICS_DATA_PREFIX?=proteomics_data
PROTEOMICS_DATA_FILE?=$(PROTEOMICS_DATA_PREFIX).txt
PROTEOMICS_ANNOT_PREFIX?=proteomics
PROTEOMICS_ANNOT_FILE?=$(PROTEOMICS_ANNOT_PREFIX)_annotation.txt

load_proteomics_annotation_%: ../common/%/proteomics.params ../common/%/proteomics/$(PROTEOMICS_ANNOT_FILE)
	GPL_ID=$*_proteomics \
	DATA_LOCATION="$(realpath ../common/$*/proteomics)" \
	KETTLE_HOME="$(realpath kettle-home)" \
	./load_proteomics_annotation.sh $< \

load_proteomics_%: ../common/%/proteomics.params load_proteomics_annotation_% ../common/%/proteomics/$(PROTEOMICS_SSM_FILE) ../common/%/proteomics/$(PROTEOMICS_CM_FILE)
	MAP_FILENAME=$(PROTEOMICS_SSM_FILE) \
	COLUMN_MAPPING_FILE=$(PROTEOMICS_CM_FILE) \
	DATA_FILE_PREFIX=$(PROTEOMICS_DATA_PREFIX) \
	STUDY_ID=$* \
	DATA_LOCATION="$(realpath ../common/$*/proteomics)" \
	KETTLE_HOME="$(realpath kettle-home)" \
	./load_proteomics.sh $<

../common/%/proteomics/$(PROTEOMICS_DATA_FILE):
	$(error Copy proteomics data into $@)

../common/%/proteomics/$(PROTEOMICS_ANNOT_FILE): ../common/%/proteomics/$(PROTEOMICS_DATA_FILE) ../common/generate_proteomics_annotation.groovy
	groovy ../common/generate_proteomics_annotation.groovy -i $< -o $@ -p $*_proteomics

../common/%/proteomics/$(PROTEOMICS_SSM_FILE): ../common/%/proteomics/$(PROTEOMICS_DATA_FILE) ../common/generate_proteomics_subject_sample_mapping.groovy
	groovy ../common/generate_proteomics_subject_sample_mapping.groovy -i $< -o $@ -p $*_proteomics -t $*

../common/%/proteomics/$(PROTEOMICS_CM_FILE): ../common/%/proteomics/$(PROTEOMICS_DATA_FILE) ../common/generate_proteomics_column_mapping.groovy
	groovy ../common/generate_proteomics_column_mapping.groovy -i $< -o $@

.PHONY: load_proteomics_annotation_% load_proteomics_%
.PRECIOUS: ../common/%/proteomics/$(PROTEOMICS_DATA_FILE) ../common/%/proteomics/$(PROTEOMICS_ANNOT_FILE) ../common/%/proteomics/$(PROTEOMICS_SSM_FILE) ../common/%/proteomics/$(PROTEOMICS_CM_FILE)

MIRNA_SSM_FILE?=mirna_subject_sample_mapping.txt
MIRNA_DATA_PREFIX?=mirna_data
MIRNA_DATA_FILE?=$(MIRNA_DATA_PREFIX).txt
MIRNA_ANNOT_PREFIX?=mirna
MIRNA_ANNOT_FILE?=$(MIRNA_ANNOT_PREFIX)_annotation.txt

load_mirna_annotation_%: ../common/%/mirna.params ../common/%/mirna ../common/%/mirna/$(MIRNA_ANNOT_FILE)
	GPL_ID=$*_mirna \
	DATA_LOCATION="$(realpath ../common/$*/mirna)" \
	KETTLE_HOME="$(realpath kettle-home)" \
	./load_mirna_annotation.sh $< \

load_mirna_%: ../common/%/mirna.params ../common/%/mirna load_mirna_annotation_% ../common/%/mirna/$(MIRNA_SSM_FILE)
	MAP_FILENAME=$(MIRNA_SSM_FILE) \
	DATA_FILE_PREFIX=$(MIRNA_DATA_PREFIX) \
	DATA_FILE=$(MIRNA_DATA_FILE) \
	STUDY_ID=$* \
	DATA_LOCATION="$(realpath ../common/$*/mirna)" \
        KETTLE_HOME="$(realpath kettle-home)" \
	./load_mirna.sh $<

../common/%/mirna:
        $(info Copy mirna data files into $*)

../common/%/mirna/$(MIRNA_ANNOT_FILE): ../common/%/mirna ../common/generate_mirna_data_annot_files.groovy
	groovy ../common/generate_mirna_data_annot_files.groovy -i $< -a $@ -d $</$(MIRNA_DATA_FILE) -p $*_mirna

../common/%/mirna/$(MIRNA_SSM_FILE): ../common/%/mirna ../common/generate_mirna_subject_sample_mapping.groovy
	groovy ../common/generate_mirna_subject_sample_mapping.groovy -i $< -o $@ -p $*_mirna -t $*

.PHONY: load_mirna_annotation_% load_mirna_%
.PRECIOUS: ../common/%/mirna/$(MIRNA_DATA_FILE) ../common/%/mirna/$(MIRNA_ANNOT_FILE) ../common/%/mirna/$(MIRNA_SSM_FILE)

../common/%/clinical: ../common/Makefile
	$(MAKE) -C ../common $*/clinical

../common/%/annotation: ../common/Makefile
	$(MAKE) -C ../common $*/annotation

../common/%/expression: ../common/Makefile
	$(MAKE) -C ../common $*/expression

../common/%/analysis: ../common/Makefile
	$(MAKE) -C ../common $*/analysis

../common/%/acgh: ../common/Makefile
	$(MAKE) -C ../common $*/acgh

../common/%/vcf: ../common/Makefile
	$(MAKE) -C ../common $*/vcf

parse_vcf_%: ../common/%/vcf.params ../common/%/vcf
	STUDY_ID=$* DATA_LOCATION="$(realpath ../common/$*/vcf)" \
			../common/_scripts/vcf/parse_vcf.sh $<

load_vcf_%: ../common/%/vcf.params parse_vcf_%
	STUDY_ID=$* PSQL='$(PSQL_COMMAND) -d $(PGDATABASE)' \
			DATA_LOCATION="$(realpath ../common/$*/vcf)" \
			./load_vcf.sh $<

clean:
	rm -f logs/*.log kettle-home/kettle.properties

.PHONY: write_kettle_properties clean

.PHONY: parse_vcf
../common/parse_vcf:
	perl ../common/_scripts/vcf/parseVCFintoTextFiles.pl "$(VCF_FILE)" "$(VCF_TEMP_DIR)" "$(DATASOURCE)" \
			"$(DATASET_ID)" "$(GPL_ID)" "$(GENOME_BUILD)" "$(ETL_USER)"
	perl ../common/_scripts/vcf/convertMappingIntoSQLFiles.pl "$(SUBJECT_SAMPLE_MAPPING_FILE)" "$(VCF_TEMP_DIR)" \
			"$(STUDY_ID)" "$(DATASET_ID)" "$(CONCEPT_PATH)"


# vim: ft=make :
