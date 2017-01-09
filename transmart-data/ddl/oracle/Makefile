SCHEMAS = \
    i2b2metadata \
    i2b2demodata \
    biomart \
    deapp \
    tm_lz \
    tm_wz \
    searchapp \
    amapp \
    fmapp \
    galaxy \
    biomart_stage \
    biomart_user \
    tm_cz

DUMP_SCHEMA_TARGETS = $(foreach s,$(SCHEMAS),dump_$(s))
LOAD_SCHEMA_TARGETS = $(foreach s,$(SCHEMAS),load_$(s))
WRITE_SYNONYM_TARGETS = $(foreach s,$(SCHEMAS),$(s)/_synonyms.sql)
WRITE_GRANT_TARGETS = $(foreach s,$(SCHEMAS),$(s)/_grants.sql)
LOAD_USER_TARGETS = $(foreach s,$(SCHEMAS),load_user_$(s))

include ../../lib/makefile.inc

DUMP_USER = groovy -cp '$(CP_ORA)' _scripts/dump_user.groovy
LOAD_DDL  = groovy -cp '$(CP_ORA)' _scripts/load_ddl.groovy
RUN_SQL   = groovy -cp '$(CP_ORA)' _scripts/run_sql.groovy

$(DUMP_SCHEMA_TARGETS):
	$(DUMP_USER) -u $(patsubst dump_%,%,$@)

dump_users:
	$(DUMP_USER) -g $(foreach s,$(SCHEMAS),-u $(s))

dump_user_objects: $(DUMP_SCHEMA_TARGETS) $(JDBC_DRIVER_ORA_PATH)

dump:
	$(MAKE) dump_users
	$(MAKE) dump_user_objects

$(LOAD_SCHEMA_TARGETS):
	$(MAKE) $(patsubst load_%,%,$@)/_synonyms.sql $(patsubst load_%,%,$@)/_grants.sql
	$(LOAD_DDL) -u $(patsubst load_%,%,$@) -j 1

$(LOAD_USER_TARGETS):
	$(RUN_SQL) GLOBAL/$(patsubst load_user_%,%,$@).sql

load_users:
	$(RUN_SQL) -j 3 $(foreach u,$(SCHEMAS),-f GLOBAL/$(u).sql)
	$(MAKE) load_passwords

load_passwords:
	php -d variables_order=E GLOBAL/passwords.sql.php $(SCHEMAS) | \
		$(RUN_SQL) --sequential

load_user_objects: $(WRITE_SYNONYM_TARGETS) $(WRITE_GRANT_TARGETS)
	$(LOAD_DDL) $(foreach s,$(SCHEMAS),-u $(s)) -j 1
	$(RUN_SQL) -f missing_commands.sql

# tablespaces.sql must be recreated everytime because its contents depend on
# an environment variable
load_tablespaces: create_tablespaces.php
	php -d variables_order=E create_tablespaces.php > GLOBAL/tablespaces.sql;
	$(RUN_SQL) -f GLOBAL/tablespaces.sql

load: create_tablespaces.php
	test '$(ORACLE_MANAGE_TABLESPACES)' != '1' || $(MAKE) load_tablespaces
	$(MAKE) load_users
	$(MAKE) load_user_objects

$(WRITE_SYNONYM_TARGETS): _scripts/generate_synonyms.php synonyms.php
	php $< synonyms.php $(patsubst %/_synonyms.sql,%,$@) > $@

$(WRITE_GRANT_TARGETS): _scripts/generate_grants.php grants.php
	php $< grants.php $(patsubst %/_grants.sql,%,$@) > $@

drop_users: drop_users.php $(JDBC_DRIVER_ORA_PATH)
	php $< $(SCHEMAS) | $(RUN_SQL) -c -j 4

drop_tablespaces: drop_tablespaces.sql
	$(RUN_SQL) -c -j 4 -f $<

$(DUMP_SCHEMA_TARGETS) dump_users $(LOAD_SCHEMA_TARGETS): $(JDBC_DRIVER_ORA_PATH)
$(LOAD_USER_TARGETS) load_tablespaces load_users drop_users drop_tablespaces: $(JDBC_DRIVER_ORA_PATH)

drop: drop_users
	test '$(ORACLE_MANAGE_TABLESPACES)' != '1' || $(MAKE) drop_tablespaces

clean_all:
	find $(SCHEMAS) GLOBAL -name '*.sql' -delete -o -name items.json -delete

runsql.sh: $(JDBC_DRIVER_ORA_PATH)
	printf '#!/bin/sh\n\n%s $$@\n' '$(RUN_SQL)' > $@
	chmod +x $@

.PHONY: clean_all $(DUMP_SCHEMA_TARGETS) dump $(LOAD_SCHEMA_TARGETS) load \
	dump_users dump_user_objects load_users load_user_objects $(LOAD_USER_TARGETS) \
	load_tablespaces drop_users drop_tablespaces drop
