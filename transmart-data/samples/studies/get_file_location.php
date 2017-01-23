<?php

if ($argc != 2) {
	fwrite(STDERR, "Syntax: php $argv[0] <study>/<study>_<type>.tar.xz\n");
	exit(1);
}

if (!preg_match('@(.+?)/\1_(.+)\.tar.xz@', $argv[1], $matches)) {
	fwrite(STDERR, "Bad argument: $argv[1]\n");
	exit(1);
}

$desired_study = $matches[1];
$desired_type = $matches[2];

foreach (new SplFileObject('./datasets') as $l) {
	if (!trim($l) || $l[0] == '#') {
		continue;
	}
	list($study, $type, $location) = preg_split('/\s+/', $l, 3);
	if ($study == $desired_study && $type == $desired_type) {
		$mult_locations = preg_split('/\s+/', $location);
		shuffle($mult_locations);
		echo implode(' ', $mult_locations), "\n";
		exit(0);
	}
}

fprintf(STDERR, "Pair (%s, %s) not found!\n", $desired_study, $desired_type);
exit(1);
