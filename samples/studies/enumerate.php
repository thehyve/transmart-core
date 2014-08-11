<?php

if (($argc != 2 && $argc != 3)
		|| ($argv[1] != 'tarballs' && $argv[1] != 'load_targets')) {
	fwrite(STDERR, "Syntax: php $argv[0] tarballs/load_targets [<type>]\n");
	exit(1);
}

$desired_type = $argc == 3 ? $argv[2] : null;

foreach (new SplFileObject(__DIR__ . '/datasets') as $l) {
	if (!trim($l) || $l[0] == '#') {
		continue;
	}
	list($study, $type, $dummy) = preg_split('/\s+/', $l, 3);
	if ($desired_type && $desired_type != $type) {
		continue;
	}
	if ($argv[1] == 'tarballs') {
		echo "${study}/${study}_${type}.tar.xz\n";
	} elseif ($argv[1] == 'load_targets') {
		echo "load_${type}_$study\n";
	}
}
