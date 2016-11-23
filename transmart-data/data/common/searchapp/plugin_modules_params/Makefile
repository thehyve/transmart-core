# assume ../plugin_module.tsv has is a dump and regenerate the files here
process_dump:
	php extract_params.php < ../plugin_module.tsv > plugin_module-template.tsv

../plugin_module.tsv: produce_tsv.php plugin_module-template.tsv $(wildcard *.params)
	php $< < plugin_module-template.tsv > $@

.PHONY: process_dump

.DELETE_ON_ERROR:
