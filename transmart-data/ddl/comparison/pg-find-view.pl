#!/bin/perl -w

$dodir="../../ddl/oracle";
opendir(ODIR,"$dodir") || die "Cannot open current directory";
foreach $dir (readdir(ODIR)) {
    if(!(-d "$dodir/$dir")) {next}
    if(!(-e "$dodir/$dir/views")) {next}
    opendir(VDIR, "$dodir/$dir/views/") || die "Failed to open $dodir/$dir/views";
    while($v = readdir(VDIR)){
	open(VIN, "$dodir/$dir/views/$v") || die "Failed to open $dodir/$dir/views/$v";
	while(<VIN>) {
	    if(/^\s+CREATE.*?VIEW (\S+)/) {
		$name = $1;
		if($name =~ /^\"([^\"]+)\"\.\"([^\"]+)\"$/) {
		    $vuser = lc($1);
		    $vname = lc($2);
		    if(defined($oview{$vname})){$oview{$vname} .= ";"}
		    $oview{$vname} .= $vuser;
		}
	    }
	}
	close VIN;
    }
    closedir VDIR;
}
closedir ODIR;

$dpdir="../../ddl/postgres";
opendir(PDIR,"$dpdir") || die "Cannot open current directory";
foreach $dir (readdir(PDIR)) {
    if(!(-d "$dpdir/$dir")) {next}
    if(!(-e "$dpdir/$dir/views")) {next}
    opendir(VDIR, "$dpdir/$dir/views/") || die "Failed to open $dpdir/$dir/views";
    while($v = readdir(VDIR)){
	open(VIN, "$dpdir/$dir/views/$v") || die "Failed to open $dpdir/$dir/views/$v";
	while(<VIN>) {
	    if(/^\s*CREATE .*?VIEW (\S+)/) {
		$vname = lc($1);
		$vuser = $dir;
		if(defined($pview{$vname})){$pview{$vname} .= ";"}
		$pview{$vname} .= $vuser;
	    }
	}
	close VIN;
    }
    closedir VDIR;
}
closedir PDIR;

opendir(DIR,".") || die "Cannot open current directory";

foreach $filename (readdir(DIR)) {
    if($filename =~ /views-([^.]+).*[.]sql$/) {
	$user = $1;
	open(IN, $filename) || die "Failed to open $filename";
	while(<IN>){
	    if(/CREATE OR REPLACE VIEW (\S+)/){
		$v = $1;
		if(defined($view{$v})) {$view{$v}.=";"}
		$view{$1}.=$user;
	    }
	}
    }
}

closedir DIR;

foreach $v (sort(keys(%oview))) {
    if(defined($view{$v})) {
	$done = "...";
	$diff = "";
	if(defined($pview{$v})){
	    $done = $pview{$v};
	    if($oview{$v} ne $pview{$v}) {$diff = "FIX "}
	}
	else {$diff = "ADD "}
	printf "%-30s %-15s %-15s %s%s\n", $v, $oview{$v}, $done, $diff, $view{$v};
    }
    elsif(defined($pview{$v})){
	printf "%-30s %-15s %-15s %s\n", $v, $oview{$v}, $pview{$v}, "UNKNOWN";
    }
    else {
	printf "%-30s %-15s UNKNOWN\n", $v, $oview{$v};
    }
}
