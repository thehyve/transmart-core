ifndef TABLESPACES
$(error TABLESPACES variable is not set)
endif

DATABASE ?= transmart
PSQL_COMMAND = $(PGSQL_BIN)psql -v ON_ERROR_STOP=1 --single-transaction
PGDUMP_COMMAND = $(PGSQL_BIN)pg_dump

