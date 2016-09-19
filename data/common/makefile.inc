# to be included from ../{oracle,postgres}/Makefile

SELF_DIR := $(dir $(lastword $(MAKEFILE_LIST)))
include $(SELF_DIR)/../../makefile.inc
COMMON_DIR := $(realpath $(SELF_DIR)/../common)
ENV_DIR := $(realpath $(SELF_DIR)/../../env)

TABLE_FILES      := $(wildcard $(COMMON_DIR)/*_list)
SCHEMA_DIRS      := $(patsubst %_list,%,$(TABLE_FILES))
SCHEMA_DIRS_DBMS := $(notdir $(SCHEMA_DIRS))
DUMP_TARGETS     := $(foreach schema,$(notdir $(SCHEMA_DIRS)),dump_schema_$(schema))
LOAD_TARGETS     := $(foreach schema,$(notdir $(SCHEMA_DIRS)),load_schema_$(schema))

# for autocompletion
$(DUMP_TARGETS) $(LOAD_TARGETS):

dump: $(DUMP_TARGETS)

load: $(LOAD_TARGETS)

$(foreach schema,$(SCHEMA_DIRS_DBMS),$(schema)/Makefile):

$(COMMON_DIR)/%/makefile.inc:
	$(MAKE) -C $(COMMON_DIR) $*/makefile.inc

.PRECIOUS: $(COMMON_DIR)/%/makefile.inc

%/Makefile: $(COMMON_DIR)/%/makefile.inc
	mkdir -p $*
	printf 'COMMON_DIR := %s\nTHIS_SCHEMA := %s\ninclude ../makefile.inc\n' \
        "$(COMMON_DIR)" $* > $@
	echo 'include ../../common/$*/makefile.inc' >> $@

dump_schema_%: %/Makefile
	$(MAKE) -C $* dump
load_schema_%: %/Makefile
	$(MAKE) -C $* load

REMOTE_FILE_STORAGE = http://files.thehyve.net

DIC_SRC_FILES_FOLDER = $(COMMON_DIR)/_dic_src
$(DIC_SRC_FILES_FOLDER):
	mkdir $@

UNIPROT_DIC_SRC_FILE = uniprot-dictionary2.tsv
UNIPROT_DIC_SRC_FILE_ARCH = $(UNIPROT_DIC_SRC_FILE).xz
$(DIC_SRC_FILES_FOLDER)/$(UNIPROT_DIC_SRC_FILE_ARCH): $(DIC_SRC_FILES_FOLDER)
	curl -f $(REMOTE_FILE_STORAGE)/dictionaries/$(UNIPROT_DIC_SRC_FILE_ARCH) > $@
$(DIC_SRC_FILES_FOLDER)/$(UNIPROT_DIC_SRC_FILE): $(DIC_SRC_FILES_FOLDER)/$(UNIPROT_DIC_SRC_FILE_ARCH)
	xz -d --stdout $< > $@

MIRNA_DIC_SRC_FOLDER = mirna-dic
MIRNA_DIC_SRC_FILE_ARCH = mirna-dictionary.tar.xz
$(DIC_SRC_FILES_FOLDER)/$(MIRNA_DIC_SRC_FILE_ARCH): $(DIC_SRC_FILES_FOLDER)
	curl -f $(REMOTE_FILE_STORAGE)/dictionaries/$(MIRNA_DIC_SRC_FILE_ARCH) > $@
$(DIC_SRC_FILES_FOLDER)/$(MIRNA_DIC_SRC_FOLDER): $(DIC_SRC_FILES_FOLDER)/$(MIRNA_DIC_SRC_FILE_ARCH)
	mkdir -p $@ && tar -C $@ -xJf $<

METABOLITE_DIC_SRC_FOLDER = metabolite-dic
METABOLITE_DIC_SRC_FILE_ARCH = Metabolite.tar.xz
$(DIC_SRC_FILES_FOLDER)/$(METABOLITE_DIC_SRC_FILE_ARCH): $(DIC_SRC_FILES_FOLDER)
	curl -f $(REMOTE_FILE_STORAGE)/dictionaries/$(METABOLITE_DIC_SRC_FILE_ARCH) > $@
$(DIC_SRC_FILES_FOLDER)/$(METABOLITE_DIC_SRC_FOLDER): $(DIC_SRC_FILES_FOLDER)/$(METABOLITE_DIC_SRC_FILE_ARCH)
	mkdir -p $@ && tar -C $@ -xJf $<

TRANSMART_LOADER = transmart-loader-latest-bamboo-snapshot
TRANSMART_LOADER_JAR = loader-jar-with-dependencies.jar
$(TRANSMART_LOADER):
	$(MAKE) -C $(ENV_DIR) $@

load_proteomics_dictionary: $(DIC_SRC_FILES_FOLDER)/$(UNIPROT_DIC_SRC_FILE) $(TRANSMART_LOADER) $(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties
	cd $(ENV_DIR)/$(TRANSMART_LOADER) \
	&& java -cp $(TRANSMART_LOADER_JAR):../../lib/* \
	org.transmartproject.pipeline.dictionary.UniProtDictionary \
	$(DIC_SRC_FILES_FOLDER)/$(UNIPROT_DIC_SRC_FILE) 

load_tworegion_sample: $(TRANSMART_LOADER) $(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties
	cd $(ENV_DIR)/$(TRANSMART_LOADER) \
	&& java -cp $(TRANSMART_LOADER_JAR):../../lib/* \
	org.transmartproject.pipeline.tworegion.TwoRegion \
	-s example_study -c "/Public studies/example_study/cga" -d b --cgaJunctions sample/tworegion/cga_junctions.csv  --cgaEvents sample/tworegion/cga_events.tsv -i subject1 -a sample1 

load_mirna_dictionary: $(DIC_SRC_FILES_FOLDER)/$(MIRNA_DIC_SRC_FOLDER) $(TRANSMART_LOADER) $(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties
	cd $(ENV_DIR)/$(TRANSMART_LOADER) \
	&& java -cp $(TRANSMART_LOADER_JAR):../../lib/* \
	org.transmartproject.pipeline.dictionary.MiRBaseDictionary \
	$(DIC_SRC_FILES_FOLDER)/$(MIRNA_DIC_SRC_FOLDER)/miRNA.dat $(DIC_SRC_FILES_FOLDER)/$(MIRNA_DIC_SRC_FOLDER)/aliases.txt

load_metabolomics_dictionary: $(DIC_SRC_FILES_FOLDER)/$(METABOLITE_DIC_SRC_FOLDER) $(TRANSMART_LOADER) $(ENV_DIR)/$(TRANSMART_LOADER)/conf/Common.properties
	cd $(ENV_DIR)/$(TRANSMART_LOADER) \
	&& java -cp $(TRANSMART_LOADER_JAR):../../lib/* \
	org.transmartproject.pipeline.dictionary.HMDBDictionary \
	$(DIC_SRC_FILES_FOLDER)/$(METABOLITE_DIC_SRC_FOLDER)/Metabolite.tsv

.PHONY: dump_schema_% load_schema_% load_proteomics_dictionary load_tworegion_sample load_mirna_dictionary load_metabolomics_dictionary
.PRECIOUS: $(foreach d,$(TABLE_DIRS),$d)


# vim: set filetype=make:
