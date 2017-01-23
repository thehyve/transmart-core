<?php
// stdin:    current file
// fildes 3: original file
// stdout:   output file

$currentLines = [];
$originalLines = [];
while (($line = fgets(STDIN)) !== false) {
	$currentLines[$line] = null;
}
$fd3 = fopen('php://fd/3', 'rb');
while (($line = fgets($fd3)) !== false) {
	$originalLines[$line] = null;
}
//fwrite(STDERR, print_r($currentLines, true));
//fwrite(STDERR, print_r($originalLines, true));

$oldCount = 0;
foreach ($originalLines as $line => $dummy) {
	if (key_exists($line, $currentLines)) {
		echo $line;
		unset($currentLines[$line]);
		$oldCount++;
	}
}

fwrite(STDERR, "Old/new lines: $oldCount/" . count($currentLines) . "\n");
foreach ($currentLines as $line => $dummy) {
	echo $line;
}
