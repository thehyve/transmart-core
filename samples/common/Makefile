DATA_LOCATION := http://files.thehyve.net/sample_data/


%/clinical: %/%_clinical.tar.xz
	$(TAR_COMMAND) -C $* -xJf $(subst %,$*,$<)

%/annotation: %/%_annotation.tar.xz
	$(TAR_COMMAND) -C $* -xJf $(subst %,$*,$<)

%/expression: %/%_expression.tar.xz
	$(TAR_COMMAND) -C $* -xJf $(subst %,$*,$<)

%/analysis: %/%_analysis.tar.xz
	$(TAR_COMMAND) -C $* -xJf $(subst %,$*,$<)

%/acgh: %/%_acgh.tar.xz
	$(TAR_COMMAND) -C $* -xJf $(subst %,$*,$<)

%/%_clinical.tar.xz %/%_annotation.tar.xz %/%_expression.tar.xz %/%_analysis.tar.xz %/%_acgh.tar.xz:
	curl -f $(DATA_LOCATION)$(notdir $(subst %,$*,$@)) > $(subst %,$*,$@)
