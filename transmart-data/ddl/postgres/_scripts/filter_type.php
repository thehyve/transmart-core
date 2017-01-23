<?php
require __DIR__ . "/classes.php";

if ($argc != 3) {
	fprintf(STDERR, "Syntax: php %s <in file> <class>\n", $argv[0]);
	exit(1);
}

$filter = new PGDumpFilter(
		new PGDumpReaderWriter($argv[1]),
		function (Item $it) use ($argv) {
			return $it->type == $argv[2];
		});

$filter->readAll();
foreach ($filter->getItems() as $item) {
	PGDumpReaderWriter::writeSingleItem(STDOUT, $item);
}
