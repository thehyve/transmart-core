DATA_LOCATION := http://files.thehyve.net/sample_data/


%/clinical: %/%_clinical.tar.xz
	tar -C $* -xJf $(subst %,$*,$<)

%/annotation: %/%_annotation.tar.xz
	tar -C $* -xJf $(subst %,$*,$<)

%/expression: %/%_expression.tar.xz
	tar -C $* -xJf $(subst %,$*,$<)

%/analysis: %/%_analysis.tar.xz
	tar -C $* -xJf $(subst %,$*,$<)

%/acgh: %/%_acgh.tar.xz
	tar -C $* -xJf $(subst %,$*,$<)

%/%_clinical.tar.xz %/%_annotation.tar.xz %/%_expression.tar.xz %/%_analysis.tar.xz %/%_acgh.tar.xz:
	curl -f $(DATA_LOCATION)$(notdir $(subst %,$*,$@)) > $(subst %,$*,$@)

.PHONY: parse_vcf
parse_vcf:
	perl ./_scripts/vcf/parseVCFintoTextFiles.pl "$(VCF_FILE)" "$(VCF_TEMP_DIR)" "$(DATASOURCE)" \
			"$(DATASET_ID)" "$(GPL_ID)" "$(GENOME_BUILD)" "$(ETL_USER)"
	perl ./_scripts/vcf/convertMappingIntoSQLFiles.pl "$(SUBJECT_SAMPLE_MAPPING_FILE)" "$(VCF_TEMP_DIR)" \
			"$(STUDY_ID)" "$(DATASET_ID)" "$(CONCEPT_PATH)"
