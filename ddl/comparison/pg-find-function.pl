#!/bin/perl -w

$dodir="../../ddl/oracle";
opendir(ODIR,"$dodir") || die "Cannot open current directory";
foreach $dir (readdir(ODIR)) {
    if(!(-d "$dodir/$dir")) {next}
    if(!(-e "$dodir/$dir/functions")) {next}
    opendir(FDIR, "$dodir/$dir/functions/") || die "Failed to open $dodir/$dir/functions";
    while($f = readdir(FDIR)){
	open(FIN, "$dodir/$dir/functions/$f") || die "Failed to open $dodir/$dir/functions/$f";
	while(<FIN>) {
	    if(/^\s+CREATE.*?FUNCTION (\S+)/) {
		$name = $1;
		if($name =~ /^\"([^\"]+)\"\.\"([^\"]+)\"$/) {
		    $fuser = lc($1);
		    $fname = lc($2);
		    if(defined($ofunction{$fname})){$ofunction{$fname} .= ";"}
		    $ofunction{$fname} .= $fuser;
		}
	    }
	}
	close FIN;
    }
    closedir FDIR;
}
closedir ODIR;


$dpdir="../../ddl/postgres";
opendir(PDIR,"$dpdir") || die "Cannot open current directory";
foreach $dir (readdir(PDIR)) {
    if(!(-d "$dpdir/$dir")) {next}
    if(!(-e "$dpdir/$dir/functions")) {next}
    opendir(FDIR, "$dpdir/$dir/functions/") || die "Failed to open $dpdir/$dir/functions";
    while($f = readdir(FDIR)){
	open(FIN, "$dpdir/$dir/functions/$f") || die "Failed to open $dpdir/$dir/functions/$f";
	print "test $dpdir/$dir/functions/$f\n";
	while(<FIN>) {
	    if(/^\s*CREATE.*?FUNCTION\s*([^ \(]+)/) {
		$fname = lc($1);
		$fuser = $dir;
		if(defined($pfunction{$fname})){$pfunction{$fname} .= ";"}
		$pfunction{$fname} .= $fuser;
	    }
	}
	close FIN;
    }
    closedir FDIR;
}
closedir PDIR;

opendir(DIR,".") || die "Cannot open current directory";

foreach $filename (readdir(DIR)) {
    if($filename =~ /functions-([^.]+).*[.]sql$/) {
	$user = $1;
	open(IN, $filename) || die "Failed to open $filename";
	while(<IN>){
	    if(/CREATE OR REPLACE FUNCTION (\S+)/){
		$f = $1;
		if(defined($function{$f})) {$function{$f}.=";"}
		$function{$1}.=$user;
	    }
	}
    }
}

closedir DIR;

foreach $f (sort(keys(%ofunction))) {
    if(defined($function{$f})) {
	$done = "...";
	$diff = "";
	if(defined($pfunction{$f})){
	    $done = $pfunction{$f};
	    if($ofunction{$f} ne $pfunction{$f}){$diff = "FIX "}
	}
	else {$diff = "ADD "}
	printf "%-30s %-15s %-15s %s%s\n", $f, $ofunction{$f}, $done, $diff, $function{$f};
    }
    elsif(defined($pfunction{$f})){
	printf "%-30s %-15s %-15s %s\n", $f, $ofunction{$f}, $pfunction{$f}, "UNKNOWN";
    }
    else {
	printf "%-30s %-15s UNKNOWN\n", $f, $ofunction{$f};
    }
}
