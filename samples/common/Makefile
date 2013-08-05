DATA_LOCATION := http://files.thehyve.net/sample_data/


%/clinical: %/%_clinical.tar.xz
	tar -C $* -xJf $(subst %,$*,$<)

%/annotation: %/%_annotation.tar.xz
	tar -C $* -xJf $(subst %,$*,$<)

%/expression: %/%_expression.tar.xz
	tar -C $* -xJf $(subst %,$*,$<)

%/analysis: %/%_analysis.tar.xz
	tar -C $* -xJf $(subst %,$*,$<)

%/%_clinical.tar.xz %/%_annotation.tar.xz %/%_expression.tar.xz %/%_analysis.tar.xz:
	curl -f $(DATA_LOCATION)$(notdir $(subst %,$*,$@)) > $(subst %,$*,$@)
