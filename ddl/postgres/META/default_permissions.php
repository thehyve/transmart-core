<?php

$permission_tr = [
	'full' => [
		'r' => 'arwdDxt', // tables, views
		'S' => 'rwU',     // sequences
		'f' => 'X',       // functions
	],
	'write' => [
		'r' => 'arwd',
		'S' => 'U',
		'f' => 'X',
	],
	'read' => [
		'r' => 'r',
		'S' => 'r',
		'f' => 'X',
	],
];

$permissions = [
	'tm_cz' => [
		'tm_cz' => 'full',
	],
	'tm_lz' => [
		'tm_cz' => 'full',
		'tm_lz' => 'full',
	],
	'tm_wz' => [
		'tm_cz' => 'full',
		'tm_wz' => 'full',
	],
	'i2b2metadata' => [
		'tm_cz' => 'full',
		'biomart_user' => 'read',
	],
	'i2b2demodata' => [
		'tm_cz' => 'full',
		'biomart_user' => 'read',
	],
	'biomart' => [
		'tm_cz' => 'full',
		'biomart_user' => 'read',
	],
	'biomart_user' => [
		'tm_cz' => 'full',
		'biomart_user' => 'full',
	],
	'deapp' => [
		'tm_cz' => 'full',
		'biomart_user' => 'read',
	],
	'amapp' => [
		'tm_cz' => 'full',
		'biomart_user' => 'read',
	],
	'fmapp' => [
		'tm_cz' => 'full',
		'biomart_user' => 'read',
	],
	'searchapp' => [
		'tm_cz' => 'full',
		'biomart_user' => 'write',
	],
	'galaxy' => [
		'tm_cz' => 'full',
		'biomart_user' => 'full',
	],
];

$stdout = fopen('php://stdout', 'w');
foreach ($permissions as $schema => $spec) {
	foreach ($spec as $user => $perm) {
		foreach (['r', 'S', 'f'] as $type) {
			$data = array($schema, $user, $type, $permission_tr[$perm][$type]);
			fputcsv($stdout, $data, "\t");
		}
	}
}
