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

$filters = [ /* {{{ */
	function (Item $item) {
		if ($item->type != 'prelude') {
			return true;
		}
		$newItem = new Item($item->type, $item->name);
		/* postgres 9.1 has no such thing: */
		$newItem->data = str_replace("SET lock_timeout = 0;\n", "", $item->data);
		return [$newItem];
	},
	function (Item $item) {
		if ($item->type != 'FUNCTION') {
			return true;
		}
		$needle = 'SET search_path TO';
		if (strpos($item->data, $needle) === false) {
			return true;
		}
		$lines = explode("\n", $item->data);
		$newFunctionItem = new Item($item->type, $item->name);
		$newSearchPathItem = new Item('FUNCTION SEARCH PATH', $item->name);
		foreach ($lines as $lineNumber => $l) {
			if (strpos($l, $needle) === false) {
				continue;
			}
			unset($lines[$lineNumber]);
			break;
		}

		$newFunctionItem->data = implode("\n", $lines);
		$newSearchPathItem->data = "ALTER FUNCTION $item->name "
			. trim($l) . ";\n";

		return [$newFunctionItem, $newSearchPathItem];
	},
]; /* }}} */

$itemSource = new PGDumpReaderWriter("_dumps/$schema.sql");
foreach ($filters as $f) {
	$itemSource = new PGDumpFilter($itemSource, $f);
}

$grouper = new PGTableGrouper(
	$itemSource,
	@$manual_objects[$schema],
	readDeps($df));
$grouper->process();
$grouper->writeResults($schema);
