<?php

if (($argc != 2 && $argc != 3)
		|| ($argv[1] != 'tarballs' && $argv[1] != 'load_targets')) {
	fwrite(STDERR, "Syntax: php $argv[0] tarballs/load_targets [<type>]\n");
	exit(1);
}

$desired_type = $argc == 3 ? $argv[2] : null;

$seen_pairs = [];
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
		$target = "load_${type}_$study";
		$seen_pairs[md5($target, true)] = null;
		echo "$target\n";
	}
}

// load stuff that was put directly on samples/studies
if ($argv[1] != 'load_targets') {
	return;
}
class DirectoryRecursiveFilterIterator extends RecursiveFilterIterator {
	function accept() {
		return $this->getDepth() <= 2 && $this->getType() == 'dir';
	}
	function hasChildren() {
		return parent::hasChildren() && $this->getDepth() < 2;
	}
	function current() {
		return explode('/', $this->getRelativePath());
	}
	private function getDepth() {
		return substr_count($this->getRelativePath(), '/') + 1;
	}
	private function getRelativePath() {
		return substr($this->key(), strlen(__DIR__) + 1);
	}
}

$it = new RecursiveIteratorIterator(
		new DirectoryRecursiveFilterIterator(
				new RecursiveDirectoryIterator(__DIR__,
						FilesystemIterator::KEY_AS_PATHNAME |
						FilesystemIterator::CURRENT_AS_FILEINFO |
						FilesystemIterator::SKIP_DOTS |
						FilesystemIterator::UNIX_PATHS)));

foreach ($it as $arr) {
	list($study, $type) = $arr;
	if ($desired_type && $desired_type != $type) {
		continue;
	}
	$target = "load_${type}_$study";
	if (!key_exists(md5($target, true), $seen_pairs)) {
		echo "$target\n";
	}
}
