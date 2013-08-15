include ../makefile.inc

ifndef TSUSER_HOME
	TSUSER_HOME = $(HOME)/
endif

FINAL_FILES = Config.groovy DataSource.groovy

all: $(FINAL_FILES)

Config-extra.php:
	cp Config-extra.php.sample $@

Config.groovy: Config-template.groovy Config-extra.php build-config.php
	php build-config.php > $@

DataSource.groovy: DataSource.groovy.php
	php -d 'variables_order=E' $< > $@

TSCONFIG = $(TSUSER_HOME).grails/transmartConfig/
install_%: %
	php backup.php '$(TSCONFIG)$*'
	cp '$<' '$(TSCONFIG)$*'

install: $(addprefix install_,$(FINAL_FILES))

clean:
	rm -f $(FINAL_FILES)

.PHONY: install clean
