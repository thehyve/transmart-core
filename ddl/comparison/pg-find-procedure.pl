#!/bin/perl -w

$dodir="../../ddl/oracle";
opendir(ODIR,"$dodir") || die "Cannot open current directory";
foreach $dir (readdir(ODIR)) {
    if(!(-d "$dodir/$dir")) {next}
    if(!(-e "$dodir/$dir/procedures")) {next}
    opendir(PDIR, "$dodir/$dir/procedures/") || die "Failed to open $dodir/$dir/procedures";
    while($p = readdir(PDIR)){
	open(PIN, "$dodir/$dir/procedures/$p") || die "Failed to open $dodir/$dir/procedures/$p";
	while(<PIN>) {
	    if(/^\s+CREATE.*?PROCEDURE (\S+)/) {
		$name = $1;
		if($name =~ /^\"([^\"]+)\"\.\"([^\"]+)\"$/) {
		    $puser = lc($1);
		    $pname = lc($2);
		    if(defined($oprocedure{$pname})){$oprocedure{$pname} .= ";"}
		    $oprocedure{$pname} .= $puser;
		}
	    }
	}
	close PIN;
    }
    closedir PDIR;
}
closedir ODIR;


$dpdir="../../ddl/postgres";
opendir(PGDIR,"$dpdir") || die "Cannot open current directory";
foreach $dir (readdir(PGDIR)) {
    if(!(-d "$dpdir/$dir")) {next}
    if(!(-e "$dpdir/$dir/functions")) {next}
    opendir(PDIR, "$dpdir/$dir/functions/") || die "Failed to open $dpdir/$dir/procedures";
    while($p = readdir(PDIR)){
	open(PIN, "$dpdir/$dir/functions/$p") || die "Failed to open $dpdir/$dir/procedures/$p";
	while(<PIN>) {
	    if(/^\s*CREATE.*?FUNCTION\s*([^ \(]+)/) {
		$pname = lc($1);
		$puser = $dir;
		if(defined($pprocedure{$pname})){$pprocedure{$pname} .= ";"}
		$pprocedure{$pname} .= $puser;
	    }
	}
	close PIN;
    }
    closedir PDIR;
}
closedir PGDIR;

opendir(DIR,".") || die "Cannot open current directory";

foreach $filename (readdir(DIR)) {
    if($filename =~ /procedures-([^.]+).*[.]sql$/) {
	$user = $1;
	open(IN, $filename) || die "Failed to open $filename";
	while(<IN>){
	    if(/CREATE OR REPLACE FUNCTION (\S+)/){
		$p = $1;
		if(defined($procedure{$p})) {$procedure{$p}.=";"}
		$procedure{$1}.=$user;
	    }
	}
    }
}

closedir DIR;

foreach $p (sort(keys(%oprocedure))) {
    if(defined($procedure{$p})) {
	$done = "...";
	$diff = "";
	if(defined($pprocedure{$p})){
	    $done = $pprocedure{$p};
	    if($oprocedure{$p} ne $pprocedure{$p}){$diff = "FIX "}
	}
	else {$diff = "ADD "}
	printf "%-30s %-15s %-15s %s%s\n", $p, $oprocedure{$p}, $done, $diff, $procedure{$p};
    }
    elsif(defined($pprocedure{$p})){
	printf "%-30s %-15s %-15s %s\n", $p, $oprocedure{$p}, $pprocedure{$p}, "UNKNOWN";
    }
    else {
	printf "%-30s %-15s UNKNOWN\n", $p, $oprocedure{$p};
    }
}
