#!/bin/csh -f

set table = $argv[1];
set ok = 1;

if (! -e ../../ddl/oracle/$table.sql) then
    echo "../../ddl/oracle/$table.sql not found"
    set ok = 0;
endif
if (-e ../../ddl/postgres/$table.sql) then
    echo "../../ddl/postges/$table.sql exists"
    set ok = 0;
endif

if(! $ok) then
    exit
endif

./table-ora-pg.pl ../../ddl/oracle/$table.sql > ../../ddl/postgres/$table.sql
more ../../ddl/postgres/$table.sql
