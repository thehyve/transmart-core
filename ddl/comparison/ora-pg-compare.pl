#!/bin/perl -w

##################################################
#
# Need to capture function return type for oracle to compare
# Need to capture and compare function arguments
# Need to catch "CREATE UNIQUE INDEX" and compare
#
##################################################

use Cwd;

%dodir = ("amapp" => "",
	  "fmapp" => "",
	  "biomart" => "",
	  "deapp" => "",
	  "searchapp" => "",
	  "tm_cz" => "",
	  "tm_lz" => "",
	  "tm_wz" => "",
	  "biomart_user" => "",
	  "i2b2demodata" => "",
	  "i2b2metadata" => "",
	  "_scripts" => "",
	  "GLOBAL" => ""
);

%dopdir = ("META" => "",
	   "support" => "",
	   "macroed_functions" => ""
);

%oparsed = ("" => 0);
%pparsed = ("" => 0);
%ounparsed = ();
%punparsed = ();

# tables and the files they are defined in
%oTableFile = ();
%pTableFile = ();

# triggers and the files they are defined in
%oTriggerFile = ();
%pTriggerFile = ();

# sequences and the files they are defined in
%oSequenceFile = ();
%pSequenceFile = ();

# functions and the files they are defined in
%oFunctionFile = ();
%pFunctionFile = ();

# functions and the type they return
%oFunctionReturn = ();
%pFunctionReturn = ();

# procedures and the files they are defined in
%oProcFile = ();

# views and the files they are defined in
%oViewFile = ();
%pViewFile = ();

# tables and their schemas
%oTableColumn = ();
%pTableColumn = ();
%oTableKey = ();
%pTableKey = ();

sub oracleParsed($$){
    my ($d,$f) = @_;
    $oparsed{$f}++; 
}

sub postgresParsed($$){
    my ($d,$f) = @_;
    $pparsed{$f}++; 
}

sub oracleUnparsed($$){
    my ($d,$f) = @_;
    $ounparsed{$f} .= "$d;"; 
}

sub postgresUnparsed($$){
    my ($d,$f) = @_;
    $punparsed{$f} .= "$d;"; 
}

sub parseOracleTop($$){
    my ($d,$f) = @_;
    local *IN;
    my $err = 0;
    my @f;

    if($f eq "Makefile") {
    }
    elsif($f eq "grants.php") {
    }
    elsif($f eq "synonyms.php") {
    }
    elsif($f eq "drop_users.php") {
    }
    elsif($f eq "create_tablespaces.php") {
    }
    elsif($f eq "drop_tablespaces.sql") {
    }
    else {
	print "Oracle parse $d/$f\n";
	return 1;
    }
    oracleUnparsed("$d/$f",$f);
    open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
    while(<IN>) {
    }
    close IN;

    return $err;
}

sub parseOracleFunctions($){
    my ($d) = @_;
    local *OSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;
    my $subd = $d;
    $subd =~ s/^$oplus\///g;

    opendir(OSDIR,"$d") || die"parseOracleFunctions failed to open $d";

    while($f = readdir(OSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if($f =~ /_bk[.]sql$/) {next}
	if($f =~ /_gsk[.]sql$/) {next}
	if(-d "$dir$d/$f") {
	    print "OracleFunctions subdir $d/$f\n";
	    next;
	}
	if($f =~ /[.]sql$/) {
#	    print "Oracle parse $d/$f\n";
	    $orsql{"$subd/$f"}++;

	    oracleParsed("$d/$f",$f);
	    open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	    while(<IN>) {
		s/\s*--.*//g;
		if(/^\s*(.*\S)\s+(FUNCTION|function)\s+([^.]+)[.](\S+)/) {
		    $fuse = $1;
		    $schema = $3;
		    $func = $4;
		    $fuse = uc($fuse);
		    $schema = uc($schema);
		    $func = uc($func);
		    $schema =~ s/\"//g;
		    $func =~ s/\"//g;
		    if($fuse =~ /^CREATE/) {
			$oFunctionFile{"$schema.$func"} = "$d/$f";
			$cfunc = 1;
		    }
		    else {
			print STDERR "$d/$f unexpected function $func     $fuse\n";
		    }
		}
	    }
	    close IN;
	}
    }
    closedir(OSDIR);
    return $err;
}

sub parseOracleProcedures($){
    my ($d) = @_;
    local *OSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;
    my $subd = $d;
    $subd =~ s/^$oplus\///g;

    opendir(OSDIR,"$d") || die"parseOracleProcedures failed to open $d";

    while($f = readdir(OSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if($f =~ /_bk[.]sql$/) {next}
	if($f =~ /_gsk[.]sql$/) {next}
	if(-d "$dir$d/$f") {
	    print "Oracleprocedures subdir $d/$f\n";
	    next;
	}
	if($f =~ /[.]sql$/) {
#	    print "Oracle parse $d/$f\n";
	    $orsql{"$subd/$f"}++;

	    oracleParsed("$d/$f",$f);
	    open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	    while(<IN>) {
		s/\s*--.*//g;
		if(/^\s*(.*\S)\s+(PROCEDURE|procedure)\s+([^.]+)[.](\S+)/) {
		    $puse = $1;
		    $schema = $3;
		    $proc = $4;
		    $puse = uc($puse);
		    $schema = uc($schema);
		    $proc = uc($proc);
		    $schema =~ s/\"//g;
		    $proc =~ s/\"//g;
		    if($puse =~ /^CREATE/) {
			$oProcFile{"$schema.$proc"} = "$d/$f";
			$cproc = 1;
		    }
		    else {
			print STDERR "$d/$f unexpected procedure $proc     $puse\n";
		    }
		}
	    }
	    close IN;
	}
    }
    closedir(OSDIR);
    return $err;
}

sub parseOracleViews($){
    my ($d) = @_;
    local *OSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;
    my $subd = $d;
    $subd =~ s/^$oplus\///g;

    opendir(OSDIR,"$d") || die"parseOracleViews failed to open $d";

    while($f = readdir(OSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if($f =~ /_bk[.]sql$/) {next}
	if($f =~ /_gsk[.]sql$/) {next}
	if(-d "$dir$d/$f") {
	    print "OracleViews subdir $d/$f\n";
	    next;
	}
	if($f =~ /[.]sql$/) {
#	    print "Oracle parse $d/$f\n";
	    $orsql{"$subd/$f"}++;

	    oracleParsed("$d/$f",$f);
	    open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	    while(<IN>) {
		s/\s*--.*//g;
		if(/^\s*(.*\S)\s+(VIEW|view)\s+([^.]+)[.](\S+)\s+(.*)/) {
		    $vuse = $1;
		    $schema = $3;
		    $view = $4;
		    $rest = $5;
		    $vuse = uc($vuse);
		    $schema = uc($schema);
		    $view = uc($view);
		    $rest = uc($rest);
		    $schema =~ s/\"//g;
		    $view =~ s/\"//g;
		    $rest =~ s/\"//g;
		    $rest =~ s/^\(//g;
		    $rest =~ s/\)$//g;
		    if($vuse =~ /^CREATE/) {
			$oViewFile{"$schema.$view"} = "$d/$f";
			$cview = 1;
		    }
		    else {
			print STDERR "$d/$f unexpected view $view     $vuse     '$rest'\n";
		    }
		}
	    }
	    close IN;
	}
    }
    return $err;
}

sub parseOracleScripts($){
    my ($d) = @_;
    local *OSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;

    opendir(OSDIR,"$d") || die"parseOracleScripts failed to open $d";

    while($f = readdir(OSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if(-d "$dir$d/$f") {
	    if($f eq "inc") {
	    }
	    else {
		print "OracleScripts subdir $d/$f\n";
	    }
	    next;
	}
	if($f =~ /[.]php$/) {
	}
	elsif($f =~ /[.]groovy$/){
	}
	else {
	    print "Oracle parse $d/$f\n";
	}
	oracleUnparsed("$d/$f",$f);
	open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	while(<IN>) {
	}
	close IN;
    }
    closedir(OSDIR);
    return $err;
}

sub parseOracle($){
    my ($d) = @_;
    local *OSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;
    my ($tuse,$schema,$table);
    my $subd = $d;
    $subd =~ s/^$oplus\///g;

    opendir(OSDIR,"$d") || die"parseOracle failed to open $d";

    while($f = readdir(OSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if($f =~ /^[#]/) {next}
	if($f =~ /_bk[.]sql$/) {next}
	if($f =~ /_gsk[.]sql$/) {next}
	if(-d "$dir$d/$f") {
	    if($f eq "functions") {
		parseOracleFunctions("$d/$f");
	    }
	    elsif($f eq "procedures"){
		parseOracleProcedures("$d/$f");
	    }
	    elsif($f eq "views"){
		parseOracleViews("$d/$f");
	    }
	    else {
		print "Oracle subdir $d/$f\n";
	    }
	    next;
	}
	if($f =~ /[.]sql$/) {
	    if($f =~ /^_.*[.]sql$/){
		if($f eq "_cross.sql"){
		}
		elsif($f eq "_misc.sql"){
		}
		elsif($f eq "util_grant_all.sql"){
		}
		else {
		    print "Oracle parse $d/$f\n";
		    next;
		}
	    }
#           print "Oracle parse $d/$f\n";
	    if($f ne "_cross.sql" && $subd ne "GLOBAL"){
		$orsql{"$subd/$f"}++;
	    }

	    oracleParsed("$d/$f",$f);

	    $ctable = 0;
	    $ctrig  = 0;
	    $cfunc  = 0;
	    $cproc  = 0;
	    $cview  = 0;
	    $cseq = 0;
	    $tseq = "";
	    $forkey = 0;

	    open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	    while(<IN>) {
		s/\s*--.*//g;
		if($ctrig) {
		    if(/select ([^.]+)[.]nextval into :NEW[.]\"([^\"]+)\" from dual;/){
			$nid = $1;
			$ncol=$2;
			$oNextval{"$schema.$table"} = "$ncol.$nid";
			$oNexttrig{"$schema.$trig"} = "$schema.$table";
		    }
		    if(/ALTER TRIGGER \S+ ENABLE/) {$ctrig = 0}
		}
		if($forkey) {
		    if(/^\s+REFERENCES \"([^\"]+)\"[.]\"([^\"]+)\" \(\"([^\"]+)\"\) (ON DELETE CASCADE )?(EN|DIS)ABLE;/) {
			$pk = " ";
			$pk .= uc($1);
			$pk .= ".";
			$pk .= uc($2);
			$pk .= "(";
			$pk .= uc($3);
			$pk .= ");";
			$oTableForkey{"$schema.$table"} .= $pk;
		    }
		    else {
			print STDERR "$d/$f Unexpected foreign key format $d/$f: $_";
		    }
		    $forkey = 0;
		}
		if(/(\S+)\s+(TABLE|table)\s+([^.]+)[.](\S+)/) {
		    $tuse = $1;
		    $schema = $3;
		    $table = $4;
		    $tuse = uc($tuse);
		    $schema = uc($schema);
		    $table = uc($table);
		    $schema =~ s/\"//g;
		    $table =~ s/\"//g;
		    if($tuse eq "CREATE") {
			$oTableFile{"$schema.$table"} = "$d/$f";
			$ctable = 1;
		    }
		    elsif($tuse eq "ALTER") {
			if(/CONSTRAINT (\S+ )FOREIGN KEY (\([^\)]+\))/){
			    $pc = $1;
			    $pk = $2;
			    $pc =~ s/\"//g;
			    if(length($pc) > 31){print STDERR "Oracle constraint length ".length($pc)." '$pc'\n"}
			    $pfk = uc($pc).uc($pk);
			    $pfk =~ s/\"//g;
			    $oTableForkey{"$schema.$table"} .= $pfk;
			    $forkey=1;
			}
		    }

		}
		elsif($ctable) {
		    if(/;/){$ctable=0; next}
		    if(/^\s*\(/){s/^\s*\(\s*//}
		    if(/^\s*\)/){$ctable=2; s/^\s*\)\s*//}
		    if(/^\s*(\"\S+)\s+(.*?),?$/) {
			$col = $1;
			$cdef = $2;
			$cdef =~ s/,\s+$//g;
			$col =~ s/\"//g;
			$oTableColumn{"$schema.$table"} .= "$col $cdef;";
		    }
		    if(/^\s*(CONSTRAINT (\S+)\s+)?PRIMARY KEY \(([^\)]+)\)/){
			$pkc = $2;
			$pk = uc($3);
			$pk =~ s/\s//g;
			$pk =~ s/\"//g;
			if(defined($pkc)){
			    $pkc = uc($pkc);
			    $pkc =~ s/\s//g;
			    $pkc =~ s/\"//g;
			    $oTableKeycon{"$schema.$table"} = $pkc;
			}
			$oTableKey{"$schema.$table"} = $pk;
		    }
		    if(/^\s*(CONSTRAINT (\S+)\s+)?UNIQUE \(([^\)]+)\)/){
			$pkc = $2;
			$pk = uc($3);
			$pk =~ s/\s//g;
			$pk =~ s/\"//g;
			if(defined($pkc)){
			    $pkc = uc($pkc);
			    $pkc =~ s/\s//g;
			    $pkc =~ s/\"//g;
			    $oTableUnikey{"$schema.$table"} .= "$pkc $pk;";
			}
			else {$oTableUnikey{"$schema.$table"} .= ". $pk;"}
		    }
		    if(/^\s*(CONSTRAINT (\S+\s+))?FOREIGN KEY (\([^\)]+\))/){
			if(defined($1)) {$pk = uc($2).uc($3)}
			else{$pk = "unnamed ".uc($3)}
			$pk =~ s/\"//g;
			$oTableForkey{"$schema.$table"} .= $pk;
			$forkey=1;
		    }
		}

		if($cseq == 1 && /([^;]*)(;?)/) {
		    $tseq .= $1;
		    if(defined($2)) {$cseq = 2}
		}
		if(/^\s*(.*\S)\s+(SEQUENCE|sequence)\s+([^.]+)[.](\S+)([^;]*)([;]?)/) {
		    $suse = $1;
		    $schema = $3;
		    $seq = $4;
		    $rest = $5;
		    $cdone = $6;
		    $suse = uc($suse);
		    $schema = uc($schema);
		    $seq = uc($seq);
		    $schema =~ s/\"//g;
		    $seq =~ s/\"//g;
#		    print "$d/$f sequence $seq     $suse\n";
		    if($suse =~ /^CREATE/) {
			$oSequenceFile{"$schema.$seq"} = "$d/$f";
			$cseq = 1;
			if(defined($cdone)){$cseq = 2}
			$tseq = $rest;
		    }
		}
		if($cseq == 2){
		    $cseq = 0;
		    $oSequenceText{"$schema.$seq"} = $tseq;
		    $tseq = "";
		}

		if(/^\s*(.*\S)\s+(TRIGGER|trigger)\s+([^.]+)[.](\S+)/) {
		    $tuse = $1;
		    $schema = $3;
		    $trig = $4;
		    $tuse = uc($tuse);
		    $schema = uc($schema);
		    $trig = uc($trig);
		    $schema =~ s/\"//g;
		    $trig =~ s/\"//g;
#		    print "$d/$f trigger $trig     $tuse\n";
		    if($tuse =~ /^CREATE/) {
			$oTriggerFile{"$schema.$trig"} = "$d/$f";
			$ctrig = 1;
#			if($trig !~ /^TRG_/){
#			    print STDERR "ctrig set for $schema.$trig\n";
#			}
		    }
		}

		if(/^\s*(.*\S)\s+(VIEW|view)\s+([^.]+)[.](\S+)\s+(.*)/) {
		    $vuse = $1;
		    $schema = $3;
		    $view = $4;
		    $rest = $5;
		    $vuse = uc($vuse);
		    $schema = uc($schema);
		    $view = uc($view);
		    $rest = uc($rest);
		    $schema =~ s/\"//g;
		    $view =~ s/\"//g;
		    $rest =~ s/\"//g;
		    $rest =~ s/^\(//g;
		    $rest =~ s/\)$//g;
		    if($vuse =~ /^CREATE/) {
			$oViewFile{"$schema.$view"} = "$d/$f";
			$cview = 1;
		    }
		    else {
			print STDERR "$d/$f unexpected view $view     $vuse     '$rest'\n";
		    }
		}
	    }
	    close IN;
	}
	elsif($f eq "items.json"){
#	    print "Oracle parse json $d/$f\n";
	    oracleUnparsed("$d/$f",$f);
	    open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	    while(<IN>) {
		if(/^\s+\"file\" : \"(\S+)\"/){
		    $ofile = $1;
		    if($ofile !~ /\/_cross[.]sql$/){
			$orload{"$ofile"}++;
		    }
		}
	    }
	    close IN;
	}
	else {
	    print "Oracle file $d/$f\n";
	}
    }
    closedir(OSDIR);
    return $err;
}

sub parsePostgresTop($$){
    my ($d,$f) = @_;
    local *IN;
    my $err = 0;
    my @f;

    if($f eq "Makefile") {
    }
    elsif($f eq "manual_objects_list.php") {
    }
    else {
	print "Postgres parse $d/$f\n";
	return 1;
    }
    postgresUnparsed("$d/$f",$f);
    open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
    while(<IN>) {
    }
    close IN;
    return $err;
}

sub parsePostgresFunctions($){
    my ($d) = @_;
    local *PSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;
    my $subd = $d;
    $subd =~ s/^$pplus\///g;

    opendir(PSDIR,"$d") || die"parsePostgresFunctions failed to open $d";

    while($f = readdir(PSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if(-d "$dir$d/$f") {
	    print STDERR "PostgresFunctions subdirectory $d/$f\n";
	    next;
	}
	if($f =~ /[.]sql$/) {
#	    print "Postgres parse $d/$f\n";
	    $pgsql{"$subd/$f"}++;
	    $noret=0;

	    postgresParsed("$d/$f",$f);
	    open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	    while(<IN>) {
		s/\s*--.*//g;
		if(/^\s*CREATE\s+FUNCTION\s+(\S+)\s*\(([^\)]*)\)\s*RETURNS (.*)/) {
		    $func = $1;
		    $ret = $2;
		    $func =~ s/\(\)$//g;
		    ($schema) = ($d =~ /\/([^\/]+)\/functions$/);
		    $func = uc($func);
		    $schema = uc($schema);
		    if($ret ne "trigger"){
			$pFunctionFile{"$schema.$func"} = "$d/$f";
			$pFunctionReturn{"$schema.$func"} = "$ret";
			$cfunc = 1;
		    }
		}
		elsif(/^\s*CREATE\s+FUNCTION\s+(\S+)\s+RETURNS (.*)/) {
		    $func = $1;
		    $ret = $2;
		    $func =~ s/\(\)$//g;
		    ($schema) = ($d =~ /\/([^\/]+)\/functions$/);
		    $func = uc($func);
		    $schema = uc($schema);
		    if($ret ne "trigger"){
			$pFunctionFile{"$schema.$func"} = "$d/$f";
			$pFunctionReturn{"$schema.$func"} = "$ret";
			$cfunc = 1;
		    }
		}
		elsif(/^\s*CREATE\s+(OR\s+REPLACE\s+)?FUNCTION\s+(\S+)\s+\($/) {
		    $func = $2;
		    $noret = 1;
		    $func =~ s/\(\)$//g;
		    ($schema) = ($d =~ /\/([^\/]+)\/functions$/);
		    $func = uc($func);
		    $schema = uc($schema);
		    $pFunctionFile{"$schema.$func"} = "$d/$f";
		}
		elsif($noret && /^\s*RETURNS (\S+) AS/) {
		    $ret = $1;
		    if($ret ne "trigger"){
			$pFunctionReturn{"$schema.$func"} = "$ret";
			$cfunc = 1;
		    }
		}
	    }
	    close IN;
	}
    }
    closedir(PSDIR);
    return $err;
}

sub parsePostgresViews($){
    my ($d) = @_;
    local *PSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;
    my $subd = $d;
    $subd =~ s/^$pplus\///g;

    opendir(PSDIR,"$d") || die"parsePostgresViews failed to open $d";

    while($f = readdir(PSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if(-d "$dir$d/$f") {
	    print "postgresViews subdir $d/$f\n";
	    next;
	}
	if($f =~ /[.]sql$/) {
#	    print "Postgres parse $d/$f\n";
	    $pgsql{"$subd/$f"}++;

	    postgresParsed("$d/$f",$f);
	    open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	    while(<IN>) {
		s/\s*--.*//g;
		if(/^\s*(.*\S)\s+(VIEW|view)\s+(\S+)/) {
		    $vuse = $1;
		    $view = $3;
		    ($schema) =  ($d =~ /\/([^\/]+)\/views$/);
		    $vuse = uc($vuse);
		    $schema = uc($schema);
		    $view = uc($view);
		    $schema =~ s/\"//g;
		    $view =~ s/\"//g;
		    if($vuse =~ /^CREATE/) {
			$pViewFile{"$schema.$view"} = "$d/$f";
			$cview = 1;
		    }
		    else {
			print STDERR "$d/$f unexpected view $view     $vuse     '$rest'\n";
		    }
		}
	    }
	    close IN;
	}
    }
    closedir(PSDIR);
    return $err;
}

sub parsePostgresScripts($){
    my ($d) = @_;
    local *PSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;

    opendir(PSDIR,"$d") || die"parsePostgresScripts failed to open $d";

    while($f = readdir(PSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if(-d "$dir$d/$f") {
	    print "PostgresScripts subdir $d/$f\n";
	    next;
	}
	if($f =~ /[.]php$/) {
	}
	else {
	    print "Postgres parse $d/$f\n";
	    next;
	}
	postgresUnparsed("$d/$f",$f);
	open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	while(<IN>) {
	}
	close IN;
    }
    closedir(PSDIR);
    return $err;
}

sub parsePostgresGlobal($){
    my ($d) = @_;
    local *PSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;

    opendir(PSDIR,"$d") || die"parsePostgresGlobal failed to open $d";

    while($f = readdir(PSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if(-d "$dir$d/$f") {
	    print "PostgresGlobal subdir $d/$f\n";
	    next;
	}
	if($f eq "Makefile") {
	}
	elsif($f =~ /[.]sql$/) {
	}
	else {
	    print "Postgres parse $d/$f\n";
	    next;
	}
	postgresUnparsed("$d/$f",$f);
	open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	while(<IN>) {
	}
	close IN;
    }
    closedir(PSDIR);
    return $err;
}

sub parsePostgresMacrofun($){
    my ($d) = @_;
    local *PSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;

    opendir(PSDIR,"$d") || die"parsePostgresMacrofun failed to open $d";

    while($f = readdir(PSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if(-d "$dir$d/$f") {
	    print "PostgresMacrofun subdir $d/$f\n";
	    next;
	}
	if($f eq "README.txt") {
	}
	elsif($f =~ /[.]sql$/) {
	}
	else {
	    print "Postgres parse $d/$f\n";
	    next;
	}
	postgresUnparsed("$d/$f",$f);
	open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	while(<IN>) {
	}
	close IN;
    }
    closedir(PSDIR);
    return $err;
}

sub parsePostgresMeta($){
    my ($d) = @_;
    local *PSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;

    opendir(PSDIR,"$d") || die"parsePostgresMeta failed to open $d";

    while($f = readdir(PSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if(-d "$dir$d/$f") {
	    print "PostgresMeta subdir $d/$f\n";
	    next;
	}
	if($f eq "Makefile") {
	}
	elsif($f =~ /[.]sql$/) {
	}
	elsif($f =~ /[.]php$/) {
	}
	elsif($f =~ /[.]tsv$/) {
	}
	else {
	    print "Postgres parse $d/$f\n";
	    next;
	}

	open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	while(<IN>) {
	}
	close IN;
    }
    closedir(PSDIR);
    return $err;
}

sub parsePostgresLoadall($){
    my ($f) = @_;
    local *IN;

    open(IN,"$f") || die "failed to open '$f";
    while(<IN>){
	if(/\\i (\S+[.]sql$)/) {
	    $pgload{"$1"}++;
	}
	else {
	    print STDERR "Unexpected line in parsePostgresLoadall $f: $_\n";
	}
    }
    close IN;
}

sub parsePostgres($){
    my ($d) = @_;
    local *PSDIR;
    local *IN;
    my $err = 0;
    my @f;
    my $f;
    my ($tuse,$schema,$table);
    my $subd = $d;
    $subd =~ s/^$pplus\///g;

    opendir(PSDIR,"$d") || die"parsePostgres failed to open $d";

    while($f = readdir(PSDIR)){
	if($f =~ /^[.]/) {next}
	if($f =~ /[~]$/) {next}
	if(-d "$dir$d/$f") {
	    if($f eq "functions") {
		parsePostgresFunctions("$d/$f");
	    }
	    elsif($f eq "views"){
		parsePostgresViews("$d/$f");
	    }
	    else {
		print "Postgres subdir $d/$f\n";
	    }
	    next;
	}
	if($f =~ /[.]sql$/) {
	    if($f =~ /^_.*[.]sql$/){
		if($f eq "_cross.sql"){ # added to postgres for RC2 port
		}
		elsif($f eq "_misc.sql"){
		}
		elsif($f eq "_load_all.sql"){
		    parsePostgresLoadall("$d/$f");
		    next;
		}
		else {
		    print "postgres parse $d/$f\n";
		    next;
		}
	    }

	    if($f ne "_cross.sql"){
		$pgsql{"$subd/$f"}++;
	    }

	    postgresParsed("$d/$f",$f);
	    $ctable = 0;
	    $ctrig  = 0;
	    $cfunc  = 0;
	    $cproc  = 0;
	    $cview  = 0;
	    $cseq = 0;
	    $alterctable = 0;
	    $altertable = "undefined";
	    $tseq = "";

	    open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	    while(<IN>) {
		s/\s*--.*//g;

		if($alterctable) {
		    if(/^\s*ADD CONSTRAINT (\S+) PRIMARY KEY \(([^\)]+)\)/) {
			$pkc = uc($1);
			$pk = uc($2);
			$pk =~ s/\s//g;
			$pk =~ s/\"//g;
			$pTableKey{$altertable} = $pk;
			if(defined($pkc)){$pTableKeycon{"$schema.$table"} = $pkc}
		    }
		    if(/^\s*ADD CONSTRAINT (\S+) UNIQUE \(([^\)]+)\)/) {
			$pkc = uc($1);
			$pk = uc($2);
			$pk =~ s/\s//g;
			$pk =~ s/\"//g;
			if(defined($pkc)){$pTableUnikey{$altertable} .= "$pkc $pk;"}
			else {$pTableUnikey{$altertable} .= ". $pk;"}
		    }
		    if(/^\s*ADD CONSTRAINT (\S+ )FOREIGN KEY (\(\S+\) )REFERENCES ([^\(]+\([^\)]+\))/){
			$pk = uc($1).uc($2);
			$pk .= uc($schema);
			$pk .= ".";
			$pk .= uc($3);
			$pk .= ";";
			$pTableForkey{"$schema.$table"} .= $pk;
		    }
		    if(/;/) {$alterctable = 0}
		}
		if(/(\S+)\s+(TABLE|table)\s+(ONLY\s+)?(\S+)/) {
		    $tuse = $1;
		    $table = $4;
		    if($table =~ /(^[.]+)[.](.*)/) {
			$schema = $1;
			$table = $2;
		    }
		    else {
			($schema) = ($d =~ /\/([^\/]+)$/);
		    }
		    $tuse = uc($tuse);
		    $schema = uc($schema);
		    $table = uc($table);
		    $schema =~ s/\"//g;
		    $table =~ s/\"//g;
		    if($tuse eq "CREATE") {
			$pTableFile{"$schema.$table"} = "$d/$f";
			$ctable = 1;
		    }
		    if($tuse eq "ALTER") {
			$altertable = "$schema.$table";
			$alterctable = 1;
		    }
		}
		elsif($ctable) {
		    if(/;/){$ctable=0; next}
		    if(/^\s*\(/){s/^\s*\(\s*//}
		    if(/^\s*\"position\"\s+/){s/\"position\"/position/} # used in de_variant_subject_idx
		    if(/^\s*\)/){$ctable=2; s/^\s*\)\s*//}
		    if(/^\s*([a-z]\S+)\s+(.*?),?$/) {
			$col = $1;
			$cdef = $2;
			$col = uc($col);
			$cdef =~ s/,\s+$//g;
			$pTableColumn{"$schema.$table"} .= "$col $cdef;";
			if($cdef =~ / DEFAULT nextval\(\'([^\']+)\'::regclass\) NOT NULL$/){
			    $cid = $1;
			    $cid = uc($1);
			    $pNextval{"$schema.$table"} = "$col.$cid";
			}
			elsif($cdef =~ /DEFAULT nextval/){print STDERR "$d/$f DEFAULT nextval not recognized: '$cdef'\n"}
		    }
		    if(/^\s*(CONSTRAINT (\S+)\s+)?PRIMARY KEY \(([^\)]+)\)/){
			$pkc = $2;
			$pk = uc($3);
			$pk =~ s/\s//g;
			$pk =~ s/\"//g;
			$pTableKey{"$schema.$table"} = $pk;
			if(defined($pkc)){
			    $pkc = uc($pkc);
			    $pkc =~ s/\s//g;
			    $pkc =~ s/\"//g;
			    $pTableKeycon{"$schema.$table"} = $pkc;
			}
		    }
		    if(/^\s*(CONSTRAINT (\S+)\s+)?UNIQUE \(([^\)]+)\)/){
			$pkc = $2;
			$pk = uc($3);
			$pk =~ s/\s//g;
			$pk =~ s/\"//g;
			if(defined($pkc)){
			    $pkc = uc($pkc);
			    $pkc =~ s/\s//g;
			    $pkc =~ s/\"//g;
			    $pTableUnikey{"$schema.$table"} .= "$pkc $pk;";
			}
			else {$pTableUnikey{"$schema.$table"} .= ". $pk;"}
		    }
		}

		if($cseq == 1 && /([^;]*)/) {
		    $tseq .= $1;
		    if(/;/) {$cseq = 2}
		}
		if(/^\s*(.*\S)\s+(SEQUENCE|sequence)\s+(\S+)(.*)/) {
		    $suse = $1;
		    $seq = $3;
		    $rest = $4;
		    $suse = uc($suse);
		    ($schema) = ($d =~ /\/([^\/]+)$/);
		    $schema = uc($schema);
		    $seq = uc($seq);
		    $schema =~ s/\"//g;
		    $seq =~ s/\"//g;
#		    print "$d/$f sequence $seq     $suse\n";
		    if($suse =~ /^CREATE/) {
			$pSequenceFile{"$schema.$seq"} = "$d/$f";
			$cseq = 1;
			$tseq = $rest;
		    }
		}
		if($cseq == 2){
		    $cseq = 0;
		    $pSequenceText{"$schema.$seq"} = $tseq;
		    $tseq = "";
		}

		if(/^\s*CREATE\s+FUNCTION\s+(\S+)\s+RETURNS trigger/) {
		    $trig = $1;
		    $trig =~ s/\(\)$//g;
		    ($schema) = ($d =~ /\/([^\/]+)$/);
		    $trig = uc($trig);
		    if($trig !~ /^TF_/ &&
			$trig !~ /_FUN$/) {print "trigger name '$trig' $f\n"}
		    $trig =~ s/^TF_//g;
		    $trig =~ s/_FUN$//g;
		    $schema = uc($schema);
#		    print "$d/$f trigger $trig     create\n";
		    $pTriggerFile{"$schema.$trig"} = "$d/$f";
		    $ctrig = 1;
		}
		if(/^\s*CREATE\s+FUNCTION\s+(\S+)\s+RETURNS (.*)/) {
		    $func = $1;
		    $ret = $2;
		    $func =~ s/\(\)$//g;
		    ($schema) = ($d =~ /\/([^\/]+)$/);
		    $func = uc($func);
		    $schema = uc($schema);
		    if($ret ne "trigger") {
			$pFunctionFile{"$schema.$func"} = "$d/$f";
			$pFunctionReturn{"$schema.$func"} = "$ret";
			$cfunc = 1;
		    }
		}
		if(/^\s*CREATE\s+FUNCTION\s+(\S+)\s*\(([^\)]*)\)\s*RETURNS (.*)/) {
		    $func = $1;
		    $ret = $2;
		    $func =~ s/\(\)$//g;
		    ($schema) = ($d =~ /\/([^\/]+)$/);
		    $func = uc($func);
		    $schema = uc($schema);
		    if($ret ne "trigger") {
			$pFunctionFile{"$schema.$func"} = "$d/$f";
			$pFunctionReturn{"$schema.$func"} = "$ret";
			$cfunc = 1;
		    }
		}
		if(/^\s*(.*\S)\s+(VIEW|view)\s+(\S+)/) {
		    $vuse = $1;
		    $view = $3;
		    ($schema) =  ($d =~ /\/([^\/]+)$/);
		    $vuse = uc($vuse);
		    $schema = uc($schema);
		    $view = uc($view);
		    $schema =~ s/\"//g;
		    $view =~ s/\"//g;
		    if($vuse =~ /^CREATE/) {
			$pViewFile{"$schema.$view"} = "$d/$f";
			$cview = 1;
		    }
		    else {
			print STDERR "$d/$f unexpected view $view     $vuse     '$rest'\n";
		    }
		}
	    }
	    close IN;
	}
	elsif($f eq "dependencies.php"){
#	    print "Postgres parse dependencies.php $d/$f\n";
	    postgresUnparsed("$d/$f",$f);
	    open(IN,"$dir$d/$f") || die "Failed to read $d/$f";
	    while(<IN>) {
	    }
	    close IN;
	}
	else {
	    print "Postgres file $d/$f\n";
	}
    }
    closedir(PSDIR);
    return $err;
}

sub compareTypes($$$$){
    my ($st,$c,$ot,$pt) = @_;
    my $s;
    my $t;
    if($ot eq $pt) {return 0}

#    if($pt =~ /DEFAULT NEXTVAL\S+/ && $ot =~ /\/\* POSTGRES NEXTVAL NEEDS TRIGGER \*\//){
#	$pt =~ s/DEFAULT NEXTVAL\S+ //g;
#	$ot =~ s/\/\*[^*]+\*\/ //g;
#    }

    if($ot =~ / NOT NULL ENABLE$/ && $pt =~ / NOT NULL$/) {
	$ot =~ s/ NOT NULL ENABLE$//;
	$pt =~ s/ NOT NULL$//;
    }

    $ot =~ s/ WITH LOCAL TIME ZONE//g; # only allows local time display, storage unchanged

    if($pt =~ /^BIGINT/) {
	if($ot =~ /^NUMBER/) {
	    $ot =~ s/^\S+/matched/;
	    $pt =~ s/^\S+/matched/;
	}
	elsif($ot =~ /^NUMBER\((\d+,0)\)/){
	    if($1 >= 9 && $1 <= 18) {
		$ot =~ s/^\S+/matched/;
		$pt =~ s/^\S+/matched/;
	    }
	}
	else {return 1}
    }

    elsif($pt =~ /^INT/) {
	if($ot =~ /^NUMBER/) {
	    $ot =~ s/^\S+/matched/;
	    $pt =~ s/^\S+/matched/;
	}
	elsif($ot =~ /^NUMBER\((\d+,0)\)/){
	    if($1 >= 5 && $1 <= 8) {
		$ot =~ s/^\S+/matched/;
		$pt =~ s/^\S+/matched/;
	    }
	}
	else {return 1}
    }

    elsif($pt =~ /^SMALLINT/) {
	if($ot =~ /^NUMBER/) {
	    $ot =~ s/^\S+/matched/;
	    $pt =~ s/^\S+/matched/;
	}
	elsif($ot =~ /^NUMBER\((\d+,0)\)/){
	    if($1 >= 1 && $1 <= 4) {
		$ot =~ s/^\S+/matched/;
		$pt =~ s/^\S+/matched/;
	    }
	}
	else {return 1}
    }

    elsif($pt =~ /^BOOLEAN DEFAULT FALSE/) { # treat boolean as never NULL
	if($ot =~ /^NUMBER\(1,0\) DEFAULT 0 NOT NULL ENABLE/){
	    $ot =~ s/^\S+ DEFAULT 0 NOT NULL ENABLE/matched/;
	    $pt =~ s/^\S+ DEFAULT FALSE/matched/;
	}
	elsif($ot =~ /^CHAR\(1 BYTE\) DEFAULT 0/){
	    $ot =~ s/^\S+ \S+ DEFAULT 0/matched/;
	    $pt =~ s/^\S+ DEFAULT FALSE/matched/;
	}
	else {return 1}
    }

    elsif($pt =~ /^BOOLEAN/) {
	if($ot =~ /^NUMBER\(1,0\)/) {
	    $ot =~ s/^\S+/matched/;
	    $pt =~ s/^\S+/matched/;
	}
	elsif($ot =~ /^CHAR\(1 BYTE\)/) {
	    $ot =~ s/^\S+ \S+/matched/;
	    $pt =~ s/^\S+/matched/;
	}
	else {return 1}
    }

    elsif($pt =~ /^DOUBLE PRECISION/) {
	if($ot =~ /^NUMBER\((\d+),(\d+)\)/){
	    if($1 >= 9 && $1 <= 18 && $2 > 0) {
		$ot =~ s/^\S+/matched/;
		$pt =~ s/^\S+ \S+/matched/;
	    }
	}
	elsif($ot =~ /^FLOAT\((\d+)\)/){ # (n) is the precision
	    if($1 > 0) {
		$ot =~ s/^\S+/matched/;
		$pt =~ s/^\S+ \S+/matched/;
	    }
	}
	elsif($ot =~ /^BINARY_DOUBLE/){ # (n) is the precision
	    $ot =~ s/^\S+/matched/;
	    $pt =~ s/^\S+ \S+/matched/;
	}
	else {return 1}
    }

    elsif($pt =~ /^NUMERIC\((\d+),(\d+)\)/) {
	$size=$1;
	$prec=$2;
	if($ot =~ /NUMBER\($size,$prec\)/){
	    $ot =~ s/^\S+/matched/;
	    $pt =~ s/^\S+/matched/;
	}
	else {return 1}
    }

    elsif($pt =~ /^NUMERIC/) {
	$size=$1;
	$prec=$2;
	if($ot =~ /NUMBER\(([*]|\d+),(\d+)\)/){
	    if($2 == 0) {
		$ot =~ s/^\S+/matched/;
		$pt =~ s/^\S+/matched/;
	    }
	}
	elsif($ot =~ /NUMBER\(/){
	    return 1;
	}
	elsif($ot =~ /NUMBER/){
	    $ot =~ s/^\S+/matched/;
	    $pt =~ s/^\S+/matched/;
	}
	else {return 1}
    }

    elsif($pt =~ /^CHARACTER VARYING\((\d+)\)/) {
	$size = $1;
	if($ot =~ /N?VARCHAR2\($size BYTE\)/){
	    $ot =~ s/^\S+ \S+/matched/;
	    $pt =~ s/^\S+ \S+/matched/;
	}
	if($ot =~ /N?VARCHAR2\($size CHAR\)/){
	    $ot =~ s/^\S+ \S+/matched/;
	    $pt =~ s/^\S+ \S+/matched/;
	}
	if($ot =~ /N?VARCHAR2\($size\)/){
	    $ot =~ s/^\S+/matched/;
	    $pt =~ s/^\S+ \S+/matched/;
	}
	if($ot =~ / DEFAULT \'([^\']*)\'$/) {
	    $oval = $1;
	    if($pt =~ /DEFAULT \'$oval\'::CHARACTER VARYING$/) {
		$ot =~ s/ DEFAULT \'([^\']*)\'$//;
		$pt =~ s/ DEFAULT \'$oval\'::CHARACTER VARYING$//;
	    }
	}
    }

    elsif($pt =~ /^CHARACTER\((\d+)\)/) {
	$size = $1;
	if($ot =~ /CHAR\($size BYTE\)/){
	    $ot =~ s/^\S+ \S+/matched/;
	    $pt =~ s/^\S+/matched/;
	}
	if($ot =~ /CHAR\($size CHAR\)/){
	    $ot =~ s/^\S+ \S+/matched/;
	    $pt =~ s/^\S+/matched/;
	}
    }

    elsif($pt =~ /^TIMESTAMP(\((\d)\))? WITHOUT TIME ZONE/){
	if(!defined($1)) {$it = 6}
	else {$it = $2}
	if($ot =~ /^DATE/) {
	    $pt =~ s/^\S+ \S+ \S+ \S+/matched/;
	    $ot =~ s/^\S+/matched/;
	}
	elsif($ot =~ /^TIMESTAMP \(9\)/ && $it == 6) {
	    $pt =~ s/^\S+ \S+ \S+ \S+/matched/;
	    $ot =~ s/^\S+ \S+/matched/;
	}
	elsif($ot =~ /^TIMESTAMP$/ && $it == 6) {
	    $pt =~ s/^\S+ \S+ \S+ \S+/matched/;
	    $ot =~ s/^\S+/matched/;
	}
	elsif($ot =~ /^TIMESTAMP \($it\)/) {
	    $pt =~ s/^\S+ \S+ \S+ \S+/matched/;
	    $ot =~ s/^\S+ \S+/matched/;
	}
	if($pt =~ / DEFAULT NOW\(\)$/ && $ot =~ / DEFAULT SYSDATE$/) {
	    $pt =~ s/ \S+ \S+$//;
	    $ot =~ s/ \S+ \S+$//;
	}
    }
    elsif($pt =~ /^TIMESTAMP(\(\d\))/){
	if($ot =~ /^TIMESTAMP \(\d\)/) {
	    $ot =~ s/^(\S+) (\S+)/$1$2/;
	}
    }
    elsif($pt =~ /^DATE\b/){
	if($ot =~ /^DATE\b/) {
	    $ot =~ s/^(\S+)/matched/;
	    $pt =~ s/^(\S+)/matched/;
	}
	if($pt =~ / DEFAULT NOW\(\)$/ && $ot =~ / DEFAULT SYSDATE$/) {
	    $pt =~ s/ \S+ \S+$//;
	    $ot =~ s/ \S+ \S+$//;
	}
    }
    elsif($pt =~ /^TEXT/){
	if($ot =~ /^N?[CB]LOB/) {
	    $pt =~ s/^\S+/matched/;
	    $ot =~ s/^\S+/matched/;
	}
    }

    if($pt =~ /DEFAULT '\S'::BPCHAR/) {
	$pt =~ s/(DEFAULT '\S')::BPCHAR/$1/;
    }

    $otrigger = "";

    if($pt =~ /^matched DEFAULT NEXTVAL\(\'([^\']+)\'::REGCLASS\)/) {
	$sq = $1;
	($s,$t) = ($st =~ /([^.]+)[.](.*)/);
	my $onv = "";
	my $pnv = "";
	if(defined($oNextval{"$s.$t"})) {$onv = $oNextval{"$s.$t"}}
	if(defined($pNextval{"$s.$t"})) {$pnv = $pNextval{"$s.$t"}}
	if($ot eq "matched" && $onv eq $pnv) {
	    $ot =~ s/ \S+$//;
	    $pt =~ s/ \S+ \S+$//;
	}
	else {
	    print STDERR "Create trigger onv: '$onv' pnv: '$pnv'\n";
	    my $trig = "TRG_$t\_$c";
	    $trig =~ s/PATIENT/PAT/g;
	    $trig =~ s/COLLECTION/COL/g;
	    $trig =~ s/QUERY_MASTER/QM/g;
	    $trig =~ s/QUERY_RESULT_INSTANCE/QRI/g;
	    $trig =~ s/QUERY_INSTANCE/QI/g;
	    $trig =~ s/QUERY_RESULT/QR/g;
	    $trig =~ s/RESULT_INSTANCE/RI/g;
	    $trig =~ s/QUERY/Q/g;
	    $trig =~ s/RESULT/RES/g;
	    $otrigger = "--
-- Type: TRIGGER; Owner: $s; Name: $trig
--
  CREATE OR REPLACE TRIGGER \"$s\".\"$trig\" 
   before insert on \"$s\".\"$t\" 
   for each row 
begin  
   if inserting then 
      if :NEW.\"$c\" is null then 
         select $sq.nextval into :NEW.\"$c\" from dual; 
      end if; 
   end if; 
end;
/
ALTER TRIGGER \"$s\".\"$trig\" ENABLE;
";

	}
    }
    if($ot eq $pt) {return 0} 

    return 1;
}

sub compareColumns($){
    my ($t) = @_;
    my @ocols = split(/;/,$oTableColumn{$t});
    my @pcols = split(/;/,$pTableColumn{$t});
    my $c;
    my %ocol = ();
    my %pcol = ();
    my $col;
    my $def;
    my $compstr = "";

    $head = "Compare table $t\n";
    foreach $c (@ocols) {
	($col, $def) = ($c =~ /^(\S+)\s+(.*)/);
	$col = uc($col);
	$def = uc($def);
	$ocol{$col} = $def;
    }

    foreach $c (@pcols) {
	($col, $def) = ($c =~ /^(\S+)\s+(.*)/);
	$col = uc($col);
	$def = uc($def);
	$pcol{$col} = $def;
    }

    $onewcol = 0;
    $pnewcol = 0;
    foreach $c (sort(keys(%ocol))) {
	if(!defined($pcol{$c})) {
	    $pnewcol++;
	    $compstr .= sprintf "$head"."column not in Postgres:  %-32s %s\n", $c, $ocol{$c};
	    $head = "";
	}
	elsif(compareTypes($t,$c,$ocol{$c},$pcol{$c})) {
	    $compstr .= sprintf "$head"."column %-32s %-45s <=> %-45s\n",
	                        $c, "'$ocol{$c}'", "'$pcol{$c}'";
            if($otrigger ne "") {$compstr .= $otrigger}
	    $head="";
	}
	else {
#	    printf "column %-32s matched\n", $c
	}
    }
    foreach $c (sort(keys(%pcol))) {
	if(!defined($ocol{$c})) {
	    $onewcol++;
	    $compstr .= sprintf "$head"."column not in Oracle:  %-32s %s\n", $c, $pcol{$c};
	    $head="";
	}
    }

    my $okey = "undefined";
    my $pkey = "undefined";
    my @okey = ();
    my @pkey = ();

    if(defined($oTableKey{$t})){$okey = $oTableKey{$t}}
    if(defined($pTableKey{$t})){$pkey = $pTableKey{$t}}
    if($okey ne $pkey) {
	$compstr .= "PRIMARY KEY     $okey    $pkey\n";
	if($okey eq "undefined") {
	    if(!defined($pTableKeycon{$t})) {
		$compstr .= "PRIMARY KEY (\"$pTableKey{$t}\")\n";
	    }else{
		$compstr .= "CONSTRAINT \"$pTableKeycon{$t}\" PRIMARY KEY (\"$pTableKey{$t}\")\n";
	    }
	}
	if($pkey eq "undefined") {
	    $lk = lc($oTableKey{$t});
	    if(!defined($oTableKeycon{$t})) {
		$compstr .= "PRIMARY KEY (lk)\n";
	    }else{
		($ts,$tt) = ($t =~ /([^.]+)[.](.*)/);
		$lc = lc($oTableKeycon{$t});
		$compstr .= "--
-- Name: $lc; Type: CONSTRAINT; Schema: $ts; Owner: -
--
ALTER TABLE ONLY $tt\n";
 		$compstr .= "    ADD CONSTRAINT $lc PRIMARY KEY ($lk);\n";
	    }
	}
    }

    $okey=0;
    $pkey=0;
    if(defined($oTableUnikey{$t})){@okey = sort(split(/;/,$oTableUnikey{$t}));$okey=$#okey+1}
    if(defined($pTableUnikey{$t})){@pkey = sort(split(/;/,$pTableUnikey{$t}));$pkey=$#pkey+1}
    if($okey && $pkey) {
	if($okey == $pkey) {
	    for ($i = 0; $i < $okey; $i++) {
		if($okey[$i] ne $pkey[$i]) {
		    $compstr .= "UNIQUE      oracle: $okey[$i]\n";
		    $compstr .= "          postgres: $pkey[$i]\n";
		}
	    }
	}
	else {
	    $compstr = "UNIQUE      count $okey    $pkey\n";
	    for ($i = 0; $i < $okey; $i++) {
		$compstr .= "            oracle: $okey[$i]\n";
	    }
	    for ($i = 0; $i < $pkey; $i++) {
		$compstr .= "          postgres: $pkey[$i]\n";
	    }
	}
    }
    elsif($#okey >= 0) {
	$compstr = "UNIQUE oracle only $okey\n";
	for ($i = 0; $i < $okey; $i++) {
	    $pk = lc($okey[$i]);
	    $compstr .= "            oracle: $pk\n";
	}
	for ($i = 0; $i < $okey; $i++) {
	    ($ts,$tt) = ($t =~ /([^.]+)[.](.*)/);
	    $ts = lc($ts);
	    $tt = lc($tt);
	    ($kn,$ki) = ($pk =~ /(\S+) (\S+)/);
	    $compstr .= "--
-- Name: $kn; Type: CONSTRAINT; Schema: $ts; Owner: -
--
ALTER TABLE ONLY $tt
    ADD CONSTRAINT $kn UNIQUE ($ki);\n"
	}
    }
    elsif($#pkey >= 0) {
	$compstr = "UNIQUE postgres only $pkey\n";
	for ($i = 0; $i < $pkey; $i++) {
	    $compstr .= "          postgres: $pkey[$i]\n";
	}
	($ts,$tt) = ($t =~ /([^.]+)[.](.*)/);
	for ($i = 0; $i < $pkey; $i++) {
	    ($kn,$ki) = ($pkey[$i] =~ /(\S+) (\S+)/);
	    $ki =~ s/,/\",\"/g;
	    $compstr .= "--
-- Type: REF_CONSTRAINT; Owner: $ts; Name: $kn
--
ALTER TABLE \"$ts\".\"$tt\"
    ADD CONSTRAINT \"$kn\" UNIQUE (\"$ki\");
";
	}
    }

#    if($okey ne $pkey) {
#	$compstr .= "UNIQUE     $okey    $pkey\n";
#	if($okey eq "undefined") {
#	    $uk = $pTableUnikey{$t};
#	    $uk =~ s/,/\",\"/g;
#	    if(!defined($pTableUnikeycon{$t})) {
#		$compstr .= "UNIQUE (\"$uk\")\n";
#	    }else{
#		$compstr .= "CONSTRAINT \"$pTableUnikeycon{$t}\" UNIQUE (\"$uk\")\n";
#	    }
#	}
#	if($pkey eq "undefined") {
#	    $lk = lc($oTableUnikey{$t});
#	    if(!defined($oTableUnikeycon{$t})) {
#		$compstr .= "UNIQUE ($lk)\n";
#	    }else{
#		($ts,$tt) = ($t =~ /([^.]+)[.](.*)/);
#		$lc = lc($oTableUnikeycon{$t});
#		$compstr .= "--
#-- Name: $lc; Type: CONSTRAINT; Schema: $ts; Owner: -
#--
#ALTER TABLE ONLY $tt
#    ADD CONSTRAINT $lc UNIQUE ($lk);\n"
#	    }
#	}
#   }

    @okey = ();
    @pkey = ();
    $okey=0;
    $pkey=0;
    if(defined($oTableForkey{$t})){@okey = sort(split(/;/,$oTableForkey{$t}));$okey=$#okey+1}
    if(defined($pTableForkey{$t})){@pkey = sort(split(/;/,$pTableForkey{$t}));$pkey=$#pkey+1}
    
    if($okey && $pkey) {
	if($okey == $pkey) {
	    for ($i = 0; $i < $okey; $i++) {
		if($okey[$i] ne $pkey[$i]) {
		    $compstr .= "FOREIGN KEY oracle: $okey[$i]\n";
		    $compstr .= "          postgres: $pkey[$i]\n";
		}
	    }
	}
	else {
	    $compstr = "FOREIGN KEY count $okey    $pkey\n";
	    for ($i = 0; $i < $okey; $i++) {
		$compstr .= "            oracle: $okey[$i]\n";
	    }
	    for ($i = 0; $i < $pkey; $i++) {
		$compstr .= "          postgres: $pkey[$i]\n";
	    }
	}
    }
    elsif($#okey >= 0) {
	$compstr = "FOREIGN KEY oracle only $okey\n";
	    for ($i = 0; $i < $okey; $i++) {
		$pk = lc($okey[$i]);
		$compstr .= "            oracle: $pk\n";
	    }
    }
    elsif($#pkey >= 0) {
	$compstr = "FOREIGN KEY postgres only $pkey\n";
	for ($i = 0; $i < $pkey; $i++) {
	    $compstr .= "          postgres: $pkey[$i]\n";
	}
	($ts,$tt) = ($t =~ /([^.]+)[.](.*)/);
	for ($i = 0; $i < $pkey; $i++) {
	    ($kn,$ki,$ks,$kt,$kr) = ($pkey[$i] =~ /(\S+) \(([^\)]+)\) ([^.]+)[.]([^\(]+)\(([^\)]+)\)/);
	    $compstr .= "--
-- Type: REF_CONSTRAINT; Owner: $ts; Name: $kn
--
ALTER TABLE \"$ts\".\"$tt\" ADD CONSTRAINT \"$kn\" FOREIGN KEY (\"$ki\")
 REFERENCES \"$ks\".\"$kt\" (\"$kr\") ENABLE;
";
	}
    }

    if($head eq "") {$compstr .= "\n"}
    return $compstr;
}

sub compareSequence($){
    my ($t) = @_;
    my $otxt = $oSequenceText{$t};
    my $ptxt = $pSequenceText{$t};
    my $compstr = "";

    my $ov;
    my $pv;

    if(!defined($otxt)){$compstr .= "No oracle sequence text for '$t'\n"}
    if(!defined($ptxt)){$compstr .= "No postgres sequence text for '$t'\n"}

    if($compstr ne "") {return $compstr}

    $otxt =~ s/\s\s+/ /g;
    $ptxt =~ s/\s\s+/ /g;

    $otxt =~ s/^\s+//g;
    $ptxt =~ s/^\s+//g;

    $otxt =~ s/\s+$//g;
    $ptxt =~ s/\s+$//g;

    $ptxt =~ s/NO MINVALUE/MINVALUE 1/g;
    $otxt =~ s/MAXVALUE 999999[9]+/MAXVALUE 999999999999999999999999999/g;
    $ptxt =~ s/NO MAXVALUE/MAXVALUE 999999999999999999999999999/g;

    $otxt =~ s/NOCACHE/CACHE 1/;
    $ptxt =~ s/NOCACHE/CACHE 1/;

    $otxt =~ s/NOORDER//;
    $otxt =~ s/NOCYCLE//;

    if($otxt eq $ptxt) {return ""}

    ($ov) = ($otxt =~ /START WITH (\S+)/);
    ($pv) = ($ptxt =~ /START WITH (\S+)/);
    if(!defined($ov) || !defined($pv) || ($ov ne $pv)){
	$compstr .= "START WITH $ov $pv\n"
    }

    ($ov) = ($otxt =~ /INCREMENT BY (\S+)/);
    ($pv) = ($ptxt =~ /INCREMENT BY (\S+)/);
    if(!defined($ov) || !defined($pv) || ($ov ne $pv)){
	$compstr .= "INCREMENT BY $ov $pv\n"
    }

    ($ov) = ($otxt =~ /MINVALUE (\S+)/);
    ($pv) = ($ptxt =~ /MINVALUE (\S+)/);
    if(!defined($ov) || !defined($pv) || ($ov ne $pv)){
	$compstr .= "MINVALUE $ov $pv\n";
    }

    ($ov) = ($otxt =~ /MAXVALUE (\S+)/);
    ($pv) = ($ptxt =~ /MAXVALUE (\S+)/);
    if(!defined($ov) || !defined($pv) || ($ov ne $pv)){
	$compstr .= "MAXVALUE $ov $pv\n";
    }

    ($ov) = ($otxt =~ /CACHE (\S+)/);
    ($pv) = ($ptxt =~ /CACHE (\S+)/);
    if(!defined($ov) || !defined($pv) || ($ov ne $pv)){
	# check for the default values specified
	if($ov ne "20" || $pv ne "1") {$compstr .= "CACHE $ov $pv\n"}
    }

    return $compstr;
}

$dir = getcwd();
$dir .= "/";
print "$dir\n";
$oplus = "../../ddl/oracle";
$pplus = "../../ddl/postgres";

%orskip = ();
%pgskip = ();

open(SKIPO, "skip_oracle.txt") || print STDERR "Unable to open skip_oracle.txt";
if(defined(SKIPO)) {
    while(<SKIPO>){
	if(/(\S+)/) {$pgskip{"$pplus/$1"}=0}
    }
    close SKIPO;
}

open(SKIPP, "skip_postgres.txt") || print STDERR "Unable to open skip_postgres.txt";
if(defined(SKIPP)) {
    while(<SKIPP>){
	if(/(\S+)/) {$orskip{"$oplus/$1"}=0}
    }
    close SKIPP;
}

# Triggers to isnore in Postgres
# e.g. logon_trigger to set Oracle user identifier
open(SKIPOT, "skip_oracle_trigger.txt") || print STDERR "Unable to open skip_oracle_trigger.txt";
if(defined(SKIPOT)) {
    while(<SKIPOT>){
	if (/(\S+)/) {$oSkipTrigger{"$1"}=0}
    }
    close SKIPOT;
}

opendir(ODIR, "$oplus") || die "Failed to open oracle DDLs";
opendir(PDIR, "$pplus") || die "Failed to open postres DDLs";

%orsql = ();
%pgsql = ();
%orload = ();
%pgload = ();

%odir = ();
$plus = $oplus;
while($d = readdir(ODIR)){
    if($d =~ /^[.]/) {next}
    if(-d "$dir$plus/$d") {
	if(defined($dodir{$d})){
	    $odir{$d} = "$plus/$d";
	}
	else {
	    print "Additional directory $plus/$d\n";
	}
    }
    else {
	parseOracleTop("$plus",$d);
    }
}

%pdir = ();
$plus = $pplus;
while($d = readdir(PDIR)){
    if($d =~ /^[.]/) {next}
    if(-d "$dir$plus/$d") {
	if(defined($dodir{$d})){
	    $pdir{$d} = "$plus/$d";
	}
	elsif(defined($dopdir{$d})){
	    $pdir{$d} = "$plus/$d";
	}
	else {
	    print "Additional directory $plus/$d\n";
	}
    }
    else {
	parsePostgresTop("$plus",$d);
    }
}


foreach $d (sort(keys(%odir))) {
    print "Oracle $d\n";
    if($d eq "_scripts"){
	parseOracleScripts($odir{$d});
    }
    else {
	parseOracle($odir{$d});
    }
}

foreach $d (sort(keys(%pdir))) {
    print "Postgres $d\n";
    if($d eq "_scripts"){
	parsePostgresScripts($pdir{$d});
    }
    elsif($d eq "macroed_functions"){
	parsePostgresMacrofun($pdir{$d});
    }
    elsif($d eq "META"){
	parsePostgresMeta($pdir{$d});
    }
    elsif($d eq "GLOBAL"){
	parsePostgresGlobal($pdir{$d});
    }
    else {
	parsePostgres($pdir{$d});
    }
}

foreach $u (sort(keys(%ounparsed))){
    @u = split(/;/,$ounparsed{$u});
    my $tot = 1 + $#u;
    if($tot > 1) {
	print "Oracle $u: $tot\n";
    }
}

foreach $u (sort(keys(%punparsed))){
    @u = split(/;/,$punparsed{$u});
    my $tot = 1 + $#u;
    if($tot > 1) {
	print "Postgres $u: $tot\n";
    }
}

$notable = 0;
$onlyotable = 0;
foreach $t (sort(keys(%oTableFile))) {
    if(defined($orskip{$oTableFile{$t}})){next}
    ++$notable;
    @ocols = split(/;/,$oTableColumn{$t});
    $nocol = 1 + $#ocols;
    if(!defined($pTableFile{$t})){
	printf "Oracle table %3d %-50s %s\n", $nocol, $t, $oTableFile{$t};
	++$onlyotable;
    }
    else {
	$compstr = compareColumns($t);
	@pcols = split(/;/,$pTableColumn{$t});
	$npcol = 1 + $#pcols;
	if($nocol != $npcol) {$diff = "MOD"}
	elsif($compstr ne ""){$diff = "CMP"}
	else {$diff = "   "}
	$pfile = $pTableFile{$t};
	$pfile =~ s/\/postgres\//\/oracle\//g;
	if($pfile eq $oTableFile{$t}) {$pfile = ""}
	else{$pfile = "   $pTableFile{$t}"}
	printf "Both %3s %3d %3d %-50s %s%s\n",
		  $diff, $npcol, $nocol, $t, $oTableFile{$t}, $pfile;
	print $compstr;
    }
}

$nptable = 0;
$onlyptable = 0;
foreach $t (sort(keys(%pTableFile))) {
    if(defined($pgskip{$pTableFile{$t}})){next}
    ++$nptable;
    if(!defined($oTableFile{$t})){
	printf "Postgres table %-50s %s\n", $t, $pTableFile{$t};
	++$onlyptable;
    }
}

$noseq = 0;
$onlyoseq = 0;
foreach $t (sort(keys(%oSequenceFile))) {
    ++$noseq;
    if(!defined($pSequenceFile{$t})){
	printf "Oracle sequence %-50s %s\n", $t, $oSequenceFile{$t};
	++$onlyoseq;
    }
    else {
	$compstr = compareSequence($t);
	$pfile = $pSequenceFile{$t};
	$pfile =~ s/\/postgres\//\/oracle\//g;
	if($pfile eq $oSequenceFile{$t}) {$pfile = ""}
	else{$pfile = "   $pSequenceFile{$t}"}
	if($compstr eq "") {$diff = "   "}
	else {$diff = "CMP"}
	printf "Both %s sequence %-50s %s%s\n", $diff, $t, $oSequenceFile{$t}, $pfile;
	print $compstr;
    }
}

$npseq = 0;
$onlypseq = 0;
foreach $t (sort(keys(%pSequenceFile))) {
    ++$npseq;
    if(!defined($oSequenceFile{$t})){
	printf "Postgres sequence %-50s %s\n", $t, $pSequenceFile{$t};
	++$onlypseq;
    }
}

$notrig = 0;
$onlyotrig = 0;
foreach $t (sort(keys(%oTriggerFile))) {
    if(defined($oSkipTrigger{$t})){next}
    ++$notrig;
    if(!defined($pTriggerFile{$t})){
# check for Postgres nextval default
# implemented as a trigger in Oracle
	$pnext=0;
	if(defined($oNexttrig{$t})){
#	    ($tn) = ($t =~ /[^.]+[.](.*)/);
	    $st = $oNexttrig{$t};
	    $nvo = $oNextval{$st};
	    if(defined($pNextval{$st})){
		$nvp = $pNextval{$st};
		if($nvo eq $nvp) {
		    $pnext=1;
		}
		else {
#		    print STDERR "Triggers mismatch '$t' '$tn' '$nvo' '$nvp'\n";
		}
	    }
	    else {
#		print STDERR "Triggers unknown '$t' '$tn' '$nvo'\n";
	    }
	}
	if(!$pnext){
	    printf "Oracle trigger %-50s %s\n", $t, $oTriggerFile{$t};
	    ++$onlyotrig;
	}
    }
    else {
	$pfile = $pTriggerFile{$t};
	$pfile =~ s/\/postgres\//\/oracle\//g;
	if($pfile eq $oTriggerFile{$t}) {$pfile = ""}
	else{$pfile = "   $pTriggerFile{$t}"}
	printf "Both   trigger %-50s %s%s\n", $t, $oTriggerFile{$t}, $pfile;
	$tfname = $t;
	$tfname =~ s/[.]/.TF_/g;
	if(!defined($pFunctionFile{"$tfname"})){
	    print STDERR "Trigger $t has no function $tfname in $pTriggerFile{$t}\n";
	}
    }
}

$nptrig = 0;
$onlyptrig = 0;
foreach $t (sort(keys(%pTriggerFile))) {
    ++$nptrig;
    if(!defined($oTriggerFile{$t})){
	printf "Postgres trigger %-50s %s\n", $t, $pTriggerFile{$t};
	++$onlyptrig;
	$tfname = $t;
	$tfname =~ s/[.]/.TF_/g;
	if(!defined($pFunctionFile{"$tfname"})){
	    print STDERR "Trigger $t has no function $tfname in $pTriggerFile{$t}\n";
	}
    }
}

$nofunc = 0;
$onlyofunc = 0;
foreach $t (sort(keys(%oFunctionFile))) {
    ++$nofunc;
    if(!defined($pFunctionFile{$t})){
	printf "Oracle function %-50s %s\n", $t, $oFunctionFile{$t};
	++$onlyofunc;
    }
    else {
	$pfile = $pFunctionFile{$t};
	$pfile =~ s/\/postgres\//\/oracle\//g;
	if($pfile eq $oFunctionFile{$t}) {$pfile = ""}
	else{$pfile = "   $pFunctionFile{$t}"}
	printf "Both   function %-50s %s%s\n", $t, $oFunctionFile{$t}, $pfile;
    }
}

$npfunc = 0;
$onlypfunc = 0;
foreach $t (sort(keys(%pFunctionFile))) {
    my $tt = $t;
    if($tt =~ /[.]TF_/) {
    $tt =~ s/[.]TF_/./;
if(defined($pTriggerFile{$tt})) {next}
}
    ++$npfunc;
    if(!defined($oFunctionFile{$t}) &&
       !defined($oProcFile{$t})){
	printf "Postgres function %-50s %s\n", $t, $pFunctionFile{$t};
	++$onlypfunc;
    }
}

$noproc = 0;
$onlyoproc = 0;
foreach $t (sort(keys(%oProcFile))) {
    ++$noproc;
    if(!defined($pFunctionFile{$t})){
	printf "Oracle procedure %-50s %s\n", $t, $oProcFile{$t};
	++$onlyoproc;
    }
    else {
	$pfile = $pFunctionFile{$t};
	$pfile =~ s/\/postgres\//\/oracle\//g;
	if($pfile eq $oProcFile{$t}) {$pfile = ""}
	else{$pfile = "   $pFunctionFile{$t}"}
	printf "Both   procedure %-50s %s%s\n", $t, $oProcFile{$t}, $pfile;
    }
}

$noview = 0;
$onlyoview = 0;
foreach $t (sort(keys(%oViewFile))) {
    ++$noview;
    if(!defined($pViewFile{$t})){
	printf "Oracle view %-50s %s\n", $t, $oViewFile{$t};
	++$onlyoview;
    }
    else {
	$pfile = $pViewFile{$t};
	$pfile =~ s/\/postgres\//\/oracle\//g;
	if($pfile eq $oViewFile{$t}) {$pfile = ""}
	else{$pfile = "   $pViewFile{$t}"}
	printf "Both   view %-50s %s%s\n", $t, $oViewFile{$t}, $pfile;
    }
}

$npview = 0;
$onlypview = 0;
foreach $t (sort(keys(%pViewFile))) {
    ++$npview;
    if(!defined($oViewFile{$t})){
	printf "Postgres view %-50s %s\n", $t, $pViewFile{$t};
	++$onlypview;
    }
}

print "\n";
print "  Oracle tables: $notable\n";
print "Postgres tables: $nptable\n";
print "  Oracle new tables: $onlyotable\n";
print "Postgres new tables: $onlyptable\n";
print "  Oracle-only columns:  $onewcol\n";
print "Postgres-only columns:  $pnewcol\n";
print "\n";

print "  Oracle sequences: $noseq\n";
print "Postgres sequences: $npseq\n";
print "  Oracle new sequences: $onlyoseq\n";
print "Postgres new sequences: $onlypseq\n";
print "\n";

print "  Oracle triggers: $notrig\n";
print "Postgres triggers: $nptrig\n";
print "  Oracle new triggers: $onlyotrig\n";
print "Postgres new triggers: $onlyptrig\n";
print "\n";

print "  Oracle functions: $nofunc\n";
print "Postgres functions: $npfunc\n";
print "  Oracle new functions: $onlyofunc\n";
print "Postgres new functions: $onlypfunc\n";
print "\n";

print "  Oracle procedures: $noproc\n";
print "  Oracle new procedures: $onlyoproc\n";
print "\n";

print "  Oracle views: $noview\n";
print "Postgres views: $npview\n";
print "  Oracle new views: $onlyoview\n";
print "Postgres new views: $onlypview\n";
print "\n";

foreach $os (sort(keys(%orsql))){
    if($orsql{$os} != 1) {
	print STDERR "$os found $orsql{$os} times\n";
    }
    if(!defined($orload{$os})) {
	print STDERR "Oracle $os not loaded\n";
    }
#    elsif($orload{$os} != 1) {
#	print STDERR "Oracle $os loaded $orload{$os} times\n";
#    }
}

foreach $os (sort(keys(%orload))){
    if(!defined($orsql{$os}) && !defined($orskip{"$oplus/$os"})) {
	print STDERR "Oracle $os unknown\n";
#	if($orload{$os} != 1) {
#	    print STDERR "Oracle $os loaded $orload{$os} times\n";
#	}
    }
}

foreach $ps (sort(keys(%pgsql))){
    if($pgsql{$ps} != 1) {
	print STDERR "Postgres $ps found $pgsql{$ps} times\n";
    }
    if(!defined($pgload{$ps})) {
	print STDERR "Postgres $ps not loaded\n";
    }
    elsif($pgload{$ps} != 1) {
	print STDERR "Postgres $ps loaded $pgload{$ps} times\n";
    }
}

foreach $ps (sort(keys(%pgload))){
    if(!defined($pgsql{$ps}) && !defined($pgskip{"$pplus/$ps"})) {
	print STDERR "Postgres $ps unknown\n";
	if($pgload{$ps} != 1) {
	    print STDERR "Postgres $ps loaded $pgload{$ps} times\n";
	}
    }
}
