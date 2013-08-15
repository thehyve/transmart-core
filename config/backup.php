<?php

if (!isset($argv[1])) {
	fwrite(STDERR, "No file path passed\n");
	exit(1);
}

$path = $argv[1];

if (!file_exists($path)) {
	return;
}

$dir = pathinfo($path, PATHINFO_DIRNAME);
$filename = pathinfo($path, PATHINFO_BASENAME);

$i = 0;
while (file_exists("$dir/$filename.$i")) { $i++; }

rename($path, "$dir/$filename.$i");
