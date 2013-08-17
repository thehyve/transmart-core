#leave kept lines in original order and add new lines at the end
#we need a temporary file because we want to read and write from
#the same file

minimize_diffs:
	/bin/bash -c ' \
	for f in `php find_chg_tsv.php`; do \
		exec 3< <(git show :./$$f); \
		temp_file=temp.$$RANDOM; \
		cp $$f $$temp_file; \
		echo minimize_diff for $$f 2>&1; \
		php -d memory_limit=1500m minimize_diff.php < $$temp_file > $$f; \
		rm $$temp_file; \
		exec 3>&-; \
	done'

.PHONY: minimize_diffs
