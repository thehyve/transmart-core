#!/usr/bin/perl -w
$out = "";
$more = "";
$indent = 0;
$schema = "todo";
$tname = "todo";
$intrig = 0;
while(<>) {
    s/ *$//g;
    if($intrig) {
	if(/^\s*CREATE( OR REPLACE)? TRIGGER \"[^\"]+\"[.]\"([^\"]+)\"/) {$trigname = lc($2)}
	if(/ insert on \S+[.]\"(\S+)\"/) {$trigtable = lc($1)}
	elsif(/ INSERT ON \S+[.]\"(\S+)\"/) {$trigtable = lc($1)}
	elsif(/ insert on \"(\S+)\"/) {$trigtable = lc($1)}
	elsif(/ INSERT ON \"(\S+)\"/) {$trigtable = lc($1)}
	if(/select (\S+)[.]nextval into :NEW[.](\S+)/) {
	    $trigseq = uc($1);
	    $trigcol = uc($2);
	    $trigseq =~ s/\"//g;
	    $trigcol =~ s/\"//g;
	}
	if(/SELECT (\S+)[.]nextval INTO :NEW[.](\S+)/) {
	    $trigseq = uc($1);
	    $trigcol = uc($2);
	    $trigseq =~ s/\"//g;
	    $trigcol =~ s/\"//g;
	}
	if(/^\s*ALTER TRIGGER \"[^\"]+\"[.]\"[^\"]+\" ENABLE;/) {
	    $intrig = 0;
	    $out .= "CREATE FUNCTION tf_$trigname() RETURNS trigger\n";
	    $out .= "    LANGUAGE plpgsql\n";
	    $out .= "    AS \$\$\n";
	    $out .= "begin\n";
	    $out .= "       if NEW.$trigcol is null then\n";
	    $out .= " select nextval('$schema.$trigseq') into NEW.$trigcol ;\n";
	    $out .= "if;\n";
	    $out .= "       RETURN NEW;\n";
	    $out .= "end;\n";
	    $out .= "\$\$;\n";
	    $out .= "\n";
	    $out .= "--\n";
	    $out .= "-- Name: $trigname(); Type: TRIGGER; Schema: $schema; Owner: -\n";
	    $out .= "--\n";
	    $out .= "  CREATE TRIGGER $trigname BEFORE INSERT ON $trigtable FOR EACH ROW EXECUTE PROCEDURE tf_$trigname();\n";
 	}
	next;
    }
    if(/^-- Type: TABLE; Owner: (\S+); Name: (\S+)/){
	$schema = lc($1);
	$checktname = lc($2);
	$out .= "-- Name: $checktname; Type: TABLE; Schema: $schema; Owner: -\n";
	next;
    }
    if(/^-- Type: INDEX; Owner: (\S+); Name: (\S+)/){
	$schema = lc($1);
	$checktname = lc($2);
	$out .= "-- Name: $checktname; Type: INDEX; Schema: $schema; Owner: -\n";
	next;
    }
    if(/^-- Type: TRIGGER; Owner: (\S+); Name: (\S+)/){
	$intrig = 1;
	$schema = lc($1);
	$checktrigname = lc($2);
	$out .= "-- Name: tf_$checktrigname; Type: FUNCTION; Schema: $schema; Owner: -\n";
	$out .= "--\n";
	next;
    }
    if(/(NO)?COMPRESS/){next}
    if(/(NO)?LOGGING/){next}
    if(/^LOB /){next}
    if(/USING INDEX/){next}
    if(/TABLESPACE \"[^\"]+\" +ENABLE/){next}
    if(/TABLESPACE \"[^\"]+\" ;/){next}
    if(/^ *TABLESPACE \"[^\"]+\"$/){next}
    if(/^\s*PRIMARY KEY \(\"([^\)]+)\"\)/){
	$pk = lc($1);
	$pk =~ s/\"//g;
	s/^\s*PRIMARY KEY \(\"([^\)]+)\"\)/PRIMARY KEY ($pk)/;
    }
    if(/^\s*UNIQUE \(\"([^\)]+)\"\)/){
	$uk = lc($1);
	$uk =~ s/\"//g;
	s/^\s*UNIQUE \(\"([^\)]+)\"\)/UNIQUE ($uk)/;
    }
    if(/^\s*CONSTRAINT \"([^\"]+)\" PRIMARY KEY \(\"([^\)]+)\"\)/){
	$con = lc($1);
	$pk = lc($2);
	$pk =~ s/\"//g;
	$more .= "\n";
	$more .= "--\n";
	$more .= "-- Name: $con; Type: CONSTRAINT; Schema: $schema; Owner: -\n";
	$more .= "--\n";
	$more .= "ALTER TABLE ONLY $tname\n";
	$more .= "    ADD CONSTRAINT $con PRIMARY KEY ($pk);\n";
	next;
    }
    if(/^\s*CONSTRAINT \"([^\"]+)\" UNIQUE \(\"([^\)]+)\"\)/){
	$con = lc($1);
	$pk = lc($2);
	$more .= "\n";
	$more .= "--\n";
	$more .= "-- Name: $con; Type: CONSTRAINT; Schema: $schema; Owner: -\n";
	$more .= "--\n";
	$more .= "ALTER TABLE ONLY $tname\n";
	$more .= "    ADD CONSTRAINT $con UNIQUE ($pk);\n";
	next;
    }
    s/NOT NULL ENABLE/NOT NULL/g;
    s/FLOAT\(\d+\)/double precision/g;
    s/N?VARCHAR2/character varying/g;
    s/ BYTE\)/)/g; 
    s/ CHAR\)/)/g; 
    if(/^\s*CREATE TABLE \"[^\"]+\"[.]\"([^\"]+)\"/) {
	$tname = lc($1);
	$indent = 4;
	$out .= "CREATE TABLE $tname (\n";
	next;
    }
    if(/^\s*CREATE INDEX \"[^\"]+\"[.]\"([^\"]+)\" ON \"[^\"]+\"[.]\"([^\"]+)\" \(\"([^\)]+)\"\)/) {
	$iname = lc($1);
	$itname = lc($2);
	$icol = lc($3);
	$icol =~ s/\"//g;
	$out .= "CREATE INDEX $iname ON $itname USING btree ($icol);\n";
	next;
    }
    s/^\s+\(\s+\"/\"/g;
    if(/^\s*\) SEGMENT.*/){
	$indent = 0;
	$out .= ");\n";
	next;
    }
    if(/^\s+TABLESPACE \"[^\"]+\"\s*;/){next}
    if(/^"([^\"]+)\"/) {
	$name = lc($1);
	s/^"([^\"]+)\"/$name/;
    }
    s/NUMBER\(18,5\)/double precision/;
    s/NUMBER\(18,0\)/bigint/;
    s/NUMBER\(38,0\)/bigint/;
    s/NUMBER\([*],0\)/bigint/;
    s/NUMBER,/bigint,/;
    s/NUMBER$/bigint/;
    s/DATE,/timestamp without time zone,/;
    s/CLOB/text/;
    s/CHAR\(/character(/;
    if($indent) {$out .= "    "}
    $out .= $_;
}

$out =~ s/,\n\)/\n)/gos;

print $out;
print $more;
