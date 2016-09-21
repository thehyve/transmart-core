#!/usr/bin/perl -w

$schema = $ARGV[0];
$func = $ARGV[1];

if(!defined($schema)) {die "Usage: schema function"}

if(-e "../../ddl/postgres/$schema/functions/$func.sql") {
    print STDERR "function file already exists\n";
    exit();
}


open (IN, "procedures-biomart_user.sql") || die "failed to find procedures-biomart_user.sql";

$copy = 0;
while(<IN>) {
    if(/CREATE OR REPLACE FUNCTION $func \(/) {
	open (OUT, ">../../ddl/postgres/$schema/functions/$func.sql")  || die "Cannot open output";
	print "Creating $schema/functions/$func.sql\n";
	print OUT "--
-- Name: $func(); Type: FUNCTION; Schema: $schema; Owner: -
--
";
	$copy = 1;
    }
    if($copy) {print OUT}
    if(/LANGUAGE PLPGSQL;/) {$copy = 0}
}
close IN;
close OUT;
