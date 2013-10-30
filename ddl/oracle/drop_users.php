<?php
for ($i = 1; $i < $argc; $i++) {
	echo "DROP USER ${argv[$i]} CASCADE;\n";
}
