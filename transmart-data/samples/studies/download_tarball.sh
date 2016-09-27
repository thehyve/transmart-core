#!/bin/bash

function main {
	local readonly destination="$1"
	shift
	local readonly urls="$@"
	local u=

	for u in $urls; do
		echo Trying "$u"
		curl -o "$destination" -L -f "$u"
		if [[ $? -eq 0 ]]; then
			exit 0
		fi
	done

	exit 1
}

main "$@"
