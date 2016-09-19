<?php
/* If we've configured PGHOST to use UNIX sockets, we can't use its value
 * when setting up jdbc urls because the jbdc driver cannot use unix sockets
 * Change it to localhost instead */
$host = (empty($_ENV['PGHOST']) || $_ENV['PGHOST'][0] == '/')
		? 'localhost'
		: $_ENV['PGHOST'];

$biomart_user_pwd = isset($_ENV['BIOMART_USER_PWD']) ?
	$_ENV['BIOMART_USER_PWD'] : 'biomart_user';

function get_env($key, $default) {
	return isset($_ENV[$key]) ? $_ENV[$key] : $default;
}
