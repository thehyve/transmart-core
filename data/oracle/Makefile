include ../common/makefile.inc

BIOMART_PSW ?= biomart
DEAPP_PSW ?= deapp
SEARCHAPP_PSW ?= searchapp
$(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties:
	echo "driver_class=oracle.jdbc.driver.OracleDriver" > $@ \
	&& echo "url=jdbc:oracle:thin:@$(ORAHOST):$(ORAPORT):$(ORASID)" >> $@ \
	&& echo "biomart_username=biomart" >> $@ \
	&& echo "biomart_password=$(BIOMART_PSW)" >> $@ \
	&& echo "deapp_username=deapp" >> $@ \
	&& echo "deapp_password=$(DEAPP_PSW)" >> $@ \
	&& echo "searchapp_username=searchapp" >> $@ \
	&& echo "searchapp_password=$(SEARCHAPP_PSW)" >> $@

config-transmart-loader: $(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties

load_proteomics_dictionary: config-transmart-loader

.PHONY: $(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties

# vim: set filetype=make:
