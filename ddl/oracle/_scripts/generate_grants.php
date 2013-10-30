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
	fprintf(STDERR, "No grants for user %s\n", $user);
	return;
}
$arr = $spec[$user];

$rights_table = [
	'FULL'  => 'ALL',
	'READ'  => 'SELECT',
	'WRITE' => 'SELECT, INSERT, UPDATE, DELETE',
];

foreach ($arr as $line) {
	list($schema, $object_name, $rights) = $line;

	if (key_exists($rights, $rights_table)) {
		$rights = $rights_table[$rights];
	}

	if ($object_name[0] == '*') {
		$object_type = substr($object_name, 1);

		echo <<<EOD
BEGIN
	FOR rec IN (SELECT object_name FROM dba_objects
		WHERE owner = '$schema' AND object_type = '$object_type')
	LOOP
		EXECUTE IMMEDIATE 'GRANT $rights ON "$schema"."' ||
				rec.object_name || '" TO "$user"';
	END LOOP;
END;
/

EOD;
	} else {
		echo <<<EOD
GRANT $rights ON "$schema"."$object_name" TO "$user";

EOD;
	}
}

