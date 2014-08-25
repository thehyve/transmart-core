#!/bin/csh -f

set table = $argv[1];
set ok = 1;
set masterdir="/data/scratch/git-london/transmart-data";

if (! -e ../../ddl/postgres/$table.sql) then
    echo "../../ddl/postgres/$table.sql not found"
    set ok = 0;
endif
if (! -e $masterdir/ddl/postgres/$table.sql) then
    echo "$masterdir/ddl/postges/$table.sql not found"
    set ok = 0;
endif

if(! $ok) then
    exit
endif

diff ../../ddl/postgres/$table.sql $masterdir/ddl/postgres/$table.sql 
