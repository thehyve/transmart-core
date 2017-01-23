#!/bin/sh

# Run the standard make to define tables and functions in ts_batch
# Fix owners, tablespaces and permissions in a follow-up script

cd ../../../ddl/postgres
export skip_ddl_global=1

make load_ts_batch
