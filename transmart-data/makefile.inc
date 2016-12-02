ifndef TABLESPACES
$(error TABLESPACES variable is not set)
endif

DATABASE ?= transmart
#we could use PGOPTIONS here
PSQL_COMMAND = $(PGSQL_BIN)psql -v ON_ERROR_STOP=1 --single-transaction -X
PSQL_COMMAND_NO_TRANSACTION = $(PGSQL_BIN)psql -v ON_ERROR_STOP=1 -X
PGDUMP_COMMAND = $(PGSQL_BIN)pg_dump
PGDUMPALL_COMMAND = $(PGSQL_BIN)pg_dumpall

TAR_COMMAND ?= tar

.DELETE_ON_ERROR:
