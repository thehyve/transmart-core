<?php

while (($data = fgetcsv(STDIN, 0, "\t", '"', '"')) !== false) {
	$filename = $data[9] /* module_name */ . ".params";

	if ($data[3] == '#placeholder#') {
		eval('$data[3] = ' . file_get_contents($filename) . ';');
		$data[3] = json_encode($data[3], JSON_UNESCAPED_SLASHES);
	}

	fputcsv(STDOUT, $data, "\t", '"');
}
