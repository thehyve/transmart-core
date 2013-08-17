<?php

//Finds tsvs under this directory with unstaged changes
$res = popen('git status --porcelain .', 'r');
while (($line = stream_get_line($res, 0, "\n")) != false) {
	if (!preg_match('@^[AM ]M data/common/(.+).tsv$@', $line, $matches)) {
		continue;
	}

	echo $matches[1], ".tsv", "\n";
}
