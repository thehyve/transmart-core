<?php

if ($argc != 2) {
	fprintf(STDERR, "Syntax: php %s <schema>\n", $argv[0]);
	exit(1);
}

$schema = $argv[1];

chdir(__DIR__ . "/..");

require __DIR__ . "/classes.php";
require __DIR__ . "/../manual_objects_list.php";

$grouper = new PGTableGrouper(
	new PGDumpReaderWriter("full_$schema.sql"),
	@$manual_objects[$schema]);
$grouper->process();
$grouper->writeResults($schema);
