<?php

chdir(__DIR__);

require 'Config-extra.php';

$templ = file_get_contents('Config-template.groovy');

foreach ($insertionPoints as $code => $content) {
    $templ = preg_replace_callback(
            '@^// ' . preg_quote($code) .  '.*$@m',
            function ($matches) {
                return "$matches[0]\n\n$GLOBALS[content]";
            },
            $templ,
            1);
}

echo $templ;

// vim: set ts=4 sw=4 ai et tw=80:
