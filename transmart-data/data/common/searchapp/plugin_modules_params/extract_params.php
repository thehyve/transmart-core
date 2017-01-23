<?php

$order = [
	'id',
	'name',
	'dataTypes',
	'dataFileInputMapping',
	'pivotData',
	'variableMapping',
	'converter',
	'processor',
	'renderer',
	'view',
];
$order = array_flip($order);
$sortingFunc = function($a, $b) use ($order) {
	return $order[$a] - $order[$b];
};


while (($data = fgetcsv(STDIN, 0, "\t", '"', '"')) !== false) {
	$filename = $data[9] /* module_name */ . ".params";
	$params = json_decode($data[3] /* params */, true);
	if ($params == null) {
		$params = [];
		fprintf(STDERR, "No params for %s\n", $data[9]);
	} else {
		uksort($params, $sortingFunc);
		file_put_contents($filename, var_export($params, true) . "\n");
		$data[3] = '#placeholder#';
	}

	fputcsv(STDOUT, $data, "\t", '"');
}
