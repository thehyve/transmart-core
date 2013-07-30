ifndef TABLESPACES
$(error TABLESPACES variable is not set)
endif

DATABASE ?= transmart
PSQL_COMMAND = $(PGSQL_BIN)psql
PGDUMP_COMMAND = $(PGSQL_BIN)pg_dump

