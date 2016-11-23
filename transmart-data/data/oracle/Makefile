include ../common/makefile.inc
include ../../lib/makefile.inc

$(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties: Common.properties.php
	php -d variables_order=E $< > $@

start_pool: $(NETTY_JAR_PATH) $(JDBC_DRIVER_ORA_PATH)
	NLS_DATE_FORMAT='yyyy-mm-dd hh24:mi:ss' \
		NLS_TIMESTAMP_FORMAT='yyyy-mm-dd hh24:mi:ss.ff' \
		groovy -cp '$(CP_ORA):$(NETTY_JAR_PATH)' start_conn_pool.groovy \
		-f pool-lock

.PHONY: start_pool $(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties

# vim: set filetype=make:
