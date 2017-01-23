<?php

$relevant_lines = array_filter(
	file(__DIR__ . '/roles.sql'),
	function ($line) {
		return preg_match('/^CREATE ROLE/', $line);
	});

$roles = array_map(function($line) {
	if (!preg_match('/^CREATE ROLE ([^;]+);$/', $line, $m)) {
		throw new Exception("Expected a match");
	}
	return $m[1];
}, $relevant_lines);

$unspec = [];
foreach ($roles as $r) {
	$env_var = strtoupper($r) . '_PWD';
	if (isset($_ENV[$env_var])) {
		$password = $_ENV[$env_var];
	} else {
		$unspec[] = $r;
		continue;
	}
	$hash = md5($password . $r);
	echo "ALTER ROLE $r WITH ENCRYPTED PASSWORD 'md5$hash';\n";
}
if ($unspec) {
	$roles_j = implode(', ', $unspec);
	fprintf(STDERR, "No password specified for $roles_j; defaults will be kept\n");
}
