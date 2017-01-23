<?php

function myfputcsv($stream, $data) {
	//php's fputcsv interprets the backslash as an escape character that, when
	//present before the enclosure, inhibits the duplication of the enclosure
	//as a means of escaping it
	//however, it is not an actual "escape" because a sequence \" will still
	//mean the same literally, not just ", unlike "" which is the escape
	//sequence for ". If then \ ia thens given as the escape character to
	//fgetcsv, the sequence \" will be inerpreted as a literal \" instead of
	//having the " end the enclosed sequence.
	//This behavior doesn't seem very useful and postgres can't deal with it

	fwrite($stream, implode("\t", array_map(function($field) {
		return '"' . str_replace('"', '""', $field) . '"';
	}, $data)) . "\n");
}

while (($data = fgetcsv(STDIN, 0, "\t", '"', '"')) !== false) {
	$filename = $data[9] /* module_name */ . ".params";

	if ($data[3] == '#placeholder#') {
		eval('$data[3] = ' . file_get_contents($filename) . ';');
		$data[3] = json_encode($data[3], JSON_UNESCAPED_SLASHES);
	}

	myfputcsv(STDOUT, $data);
}
