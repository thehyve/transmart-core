THIS_SCHEMA := amapp
include $(COMMON_DIR)/makefile_schemas.inc

load: load_am_tag_item load_am_tag_template
.PHONY: load

dump: dump_am_tag_item dump_am_tag_template
.PHONY: dump
