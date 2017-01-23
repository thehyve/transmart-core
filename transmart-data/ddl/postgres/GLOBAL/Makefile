include ../../../makefile.inc

TBNAMES = transmart \
          indx

TBDIR_TARGETS = $(addprefix $(TABLESPACES), $(TBNAMES))
TB_TARGETS = $(addprefix tablespace_, $(TBNAMES))
TBDROP_TARGETS = $(addprefix drop_tb_, $(TBNAMES))

cluster_dump.sql:
	$(PGDUMPALL_COMMAND) -g > $@

general_db_dump.sql:
	$(PGDUMP_COMMAND) -s -x -N i2b2demodata -N i2b2metadata -N tm_lz \
			-N tm_cz -N tm_wz -N searchapp -N biomart \
			-N public -N amapp -N fmapp -N galaxy -N ts_batch > $@

roles.sql: cluster_dump.sql
	php ../_scripts/filter_type.php $< "Roles" | \
			egrep 'ROLE (biomart_user|deapp|i2b2metadata|i2b2demodata|tm_cz|tm_lz|tm_wz|biomart|searchapp|amapp|fmapp|galaxy|ts_batch)\b' \
			> $@

extensions.sql: general_db_dump.sql
	php ../_scripts/filter_type.php $< "EXTENSION" > $@

$(TBDIR_TARGETS):
	mkdir -p '$@'
	@echo "The directory $@ was created, but I did not check whether its owner matches PostgreSQL system user"

# sometimes the user can't know if the tablespace directory exists because he
# lacks execute permissions in one of parent directories of $(TABLESPACES) (the
# tablespaces directory). Therefore, make make look whether the directory exists
# only if we need to create the tablespace. We accomplish this by not making
# this rule not depend directly on $(TABLESPACES)% and instead call
# $(MAKE) $(TABLESPACES)$* at the last moment
tablespace_%:
	test `$(PSQL_COMMAND) -c "select exists (select spcname from pg_tablespace where spcname = '$*')" -tA` \
			= 't' || { \
			$(MAKE) $(TABLESPACES)$*; \
			$(PSQL_COMMAND) -c "CREATE TABLESPACE $* LOCATION '$(TABLESPACES)$*'"; }

tablespaces: $(TB_TARGETS)

createdb:
	test `$(PSQL_COMMAND) -d template1 -c "select exists (select datname from pg_database where datname = '$(PGDATABASE)')" -tA` \
			= 'f' || (echo -e "\e[31mERROR\e[m: Database $(PGDATABASE) already exists or connection error"; false)
	$(PSQL_COMMAND) -d template1 -c "CREATE DATABASE $(PGDATABASE) TEMPLATE template0 \
			ENCODING='UTF-8' LC_COLLATE='en_US.UTF-8'"

# Don't depend on the SQL file, otherwise make'll try to create it
load_%:
	$(PSQL_COMMAND) -f $*.sql

load: load_cast_varchar_numeric load_roles load_extensions tablespaces
	$(MAKE) load_passwords

load_passwords:
	php -d variables_order=E passwords.sql.php | $(PSQL_COMMAND)

drop_roles:
	$(PSQL_COMMAND) -d template1 -f drop_roles.sql

drop_database:
	$(PSQL_COMMAND) -d template1 -c "DROP DATABASE IF EXISTS $(PGDATABASE)"

$(TBDROP_TARGETS):
	$(PSQL_COMMAND) -d template1 -c "DROP TABLESPACE IF EXISTS $(subst drop_tb_,,$@)"

drop_tablespaces: $(TBDROP_TARGETS)

drop: drop_database drop_tablespaces drop_roles

clean:
	rm -f cluster_dump.sql general_db_dump.sql

clean_generated: clean
	rm -f roles.sql

.PHONY: $(TBDROP_TARGETS)
.PHONY: createdb clean_generated clean tablespaces load drop_roles \
	drop_database drop_tablespaces drop load_passwords
