#!/usr/bin/perl -w

$schema = $ARGV[0];
$view = $ARGV[1];

if(!defined($schema)) {die "Usage: schema view"}

if(-e "../../ddl/postgres/$schema/views/$view.sql") {
    print STDERR "view file already exists\n";
    exit();
}


open (IN, "views-biomart_user.sql") || die "failed to find views-biomart_user.sql";

$copy = 0;
while(<IN>) {
    if(/CREATE OR REPLACE VIEW $view \(/) {
	open (OUT, ">../../ddl/postgres/$schema/views/$view.sql")  || die "Cannot open output";
	print "Creating $schema/views/$view.sql\n";
	print OUT "--
-- Name: $view; Type: VIEW; Schema: $schema; Owner: -
--
";
	$copy = 1;
	$comma = 0;
    }
    if($copy) {print OUT}
    if(/;/){$comma=1)
    if($copy && $comma && /CREATE .*VIEW/) {$copy = 0;$comma=0}
}
close IN;
close OUT;
