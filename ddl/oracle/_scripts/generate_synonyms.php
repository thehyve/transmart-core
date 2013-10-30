<?php

if ($argc != 3) {
	fprintf(STDERR, "%s", "Syntax: $argv[0] <spec file> <user> > <out file>\n");
	exit(1);
}

$file = $argv[1];
$user = strtoupper($argv[2]);

if (!file_exists($file)) {
	fprintf(STDERR, "No such file: %s\n", $file);
	exit(1);
}

require $file;

if (!isset($spec)) {
	fprintf(STDERR, "File %s did not specify variable \$spec\n", $file);
	exit(1);
}

if (!key_exists($user, $spec)) {
	fprintf(STDERR, "No synonyms for user %s\n", $user);
	return;
}
$arr = $spec[$user];

foreach ($arr as $line) {
	list($schema, $object_name) = $line;

	if ($object_name[0] == '*') {
		$object_type = substr($object_name, 1);

		echo <<<EOD
BEGIN
	FOR rec IN (SELECT object_name FROM dba_objects
		WHERE owner = '$schema' AND object_type = '$object_type')
	LOOP
		EXECUTE IMMEDIATE 'CREATE OR REPLACE SYNONYM "$user"."' ||
				rec.object_name || '" FOR "$schema"."' ||
				rec.object_name || '"';
	END LOOP;
END;
/

EOD;
	} else {
		echo <<<EOD
CREATE OR REPLACE SYNONYM "$user"."$object_name" FOR "$schema"."$object_name";

EOD;
	}
}

