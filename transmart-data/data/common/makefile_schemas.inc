ifeq ($(THIS_SCHEMA), searchapp)
#special case
load_plugin_module: load_plugin update_plugin_module
load: load_plugin_module

update_plugin_module:
	$(MAKE) -C ../../common/searchapp/plugin_modules_params ../plugin_module.tsv

.PHONY: update_plugin_module
endif

ifdef THIS_SCHEMA
SCHEMA_COMP := '$(THIS_SCHEMA)'
else
SCHEMA_COMP := IN ('i2b2demodata', 'i2b2metadata', 'deapp', 'biomart', 'searchapp', 'amapp')
endif
