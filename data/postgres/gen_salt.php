<?php
$encoded_len = 22;
$raw_len = floor($encoded_len * 3 / 4 + 1);

$urand = fopen('/dev/urandom', 'rb');
$raw_salt = fread($urand, $raw_len);
echo str_replace('+', '.', substr(base64_encode($raw_salt), 0, $encoded_len));
