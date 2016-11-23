<?php
array_shift($argv);
$roles = $argv;
$unspec = [];
foreach ($roles as $r) {
	$env_var = strtoupper($r) . '_PWD';
	if (isset($_ENV[$env_var])) {
		$password = $_ENV[$env_var];
	} else {
		continue;
	}
	$escaped_password = str_replace('"', '""', $password);
	echo "ALTER USER $r IDENTIFIED BY \"$escaped_password\"\n";
}
if ($unspec) {
	$roles_j = implode(', ', $unspec);
	fprintf(STDERR, "No password specified for $roles_j; defaults will be kept\n");
}
