include ../makefile.inc

ifndef TSUSER_HOME
	TSUSER_HOME = $(HOME)/
endif

FINAL_FILES = Config.groovy DataSource.groovy BuildConfig.groovy

all: $(FINAL_FILES)

Config-extra.php BuildConfig.groovy:
	cp $@.sample $@

Config.groovy: Config-template.groovy Config-extra.php build-config.php
	php build-config.php > $@

DataSource.groovy: DataSource.groovy.php
	php -d 'variables_order=E' $< > $@

TSCONFIG = $(TSUSER_HOME).grails/transmartConfig/
install_%: %
	@test -d $(TSUSER_HOME).grails || /bin/echo -e \
		"\e[33mWARNING\e[m: $(TSUSER_HOME).grails does not exist; I'll attempt to create it, but make sure it'll have the right permissions afterwards" >&2
	@test ! -f '$<.sample' || test ! '$<.sample' -nt '$<' || /bin/echo  -e\
		"\e[33mWARNING\e[m: $<.sample is newer than $<; it may need to be updated" >&2
	mkdir -p $(TSCONFIG)
	php backup.php '$(TSCONFIG)$*'
	cp '$<' '$(TSCONFIG)$*'

install: $(addprefix install_,$(FINAL_FILES))

clean:
	rm -f $(FINAL_FILES)

.PHONY: install clean DataSource.groovy
