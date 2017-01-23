SELF_DIR := $(dir $(lastword $(MAKEFILE_LIST)))

DATA_LOCATION := https://s3.amazonaws.com/thehyve-data/public/Reference%2520data

DE_RC_SNP_INFO_FILE := $(SELF_DIR)/de_rc_snp_info_b146_GRCh37p13.tsv.lz4
DE_GENE_INFO_FILE := $(SELF_DIR)/de_gene_info_GRCh37p13.tsv.lz4
RECOMBINATION_FILE_TARBALL := $(SELF_DIR)/genetic_map_HapMapII_GRCh37.tar.gz
RECOMBINATION_FILE := $(SELF_DIR)/bio_recombination_rates.tsv.lz4
DE_SNP_GENE_MAP_SQL := $(SELF_DIR)/insert_snp_gene_map.sql

$(DE_RC_SNP_INFO_FILE) $(DE_GENE_INFO_FILE) $(RECOMBINATION_FILE_TARBALL):
	curl -L -f '$(DATA_LOCATION)'/$(notdir $@) > $@

$(RECOMBINATION_FILE): $(RECOMBINATION_FILE_TARBALL)
	$(SELF_DIR)/build_recombination_file $< | lz4c > $@

# recipes to be defined in rdbms specific makefile
load_de_rc_snp_info: $(DE_RC_SNP_INFO_FILE)
load_de_gene_info: $(DE_GENE_INFO_FILE)
load_bio_recombination_rates: $(RECOMBINATION_FILE)
load_de_snp_gene_map: load_de_rc_snp_info

load_gwas_reference: load_de_rc_snp_info load_de_gene_info \
	load_bio_recombination_rates

.PHONY: load_gwas_reference load_de_rc_snp_info load_de_gene_info \
	load_bio_recombination_rates 

# vim: set ft=make:
