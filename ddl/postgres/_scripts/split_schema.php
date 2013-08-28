<?php

if ($argc != 2) {
	fprintf(STDERR, "Syntax: php %s <schema>\n", $argv[0]);
	exit(1);
}

$schema = $argv[1];

chdir(__DIR__ . "/..");

require __DIR__ . "/classes.php";
require __DIR__ . "/../manual_objects_list.php";

$depsFile = "_dumps/${schema}_deps.tsv";
if (($df = fopen($depsFile, "rb")) === false) {
	fprintf(STDERR, "Could not open %s\n", $depsFile);
	exit(1);
}

function readDeps($df) {
	$seededDependencies = [];
	$cols = [
		'objtype',
		'objschema',
		'objname',
		'reftype',
		'refschema',
		'refname',
	];
	$cols = array_flip($cols);

	while (($line = fgetcsv($df, 0, "\t", '"', '"')) !== false) {
		/* only views' dependencies dumped now */
		assert($line[$cols['objtype']] == 'v');
		$type = 'VIEW';

		/* only dependencies of type table and view supported */
		assert(in_array($line[$cols['reftype']], ['v', 'r']));
		$reftype = $line[$cols['reftype']] == 'v' ? 'VIEW' : 'TABLE';

		$refschema = $line[$cols['refschema']] == $line[$cols['objschema']]
			?  ''
			: ($line[$cols['refschema']] . ".");

		$seededDependencies["VIEW\0{$line[$cols["objname"]]}"][] =
			"$reftype\0$refschema{$line[$cols["refname"]]}";
	}
	return $seededDependencies;
}

$grouper = new PGTableGrouper(
	new PGDumpReaderWriter("_dumps/$schema.sql"),
	@$manual_objects[$schema],
	readDeps($df));
$grouper->process();
$grouper->writeResults($schema);
