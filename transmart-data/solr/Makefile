include ../makefile.inc

start: solr_home
	java -jar start.jar

include ../lib/makefile.inc

SOLR_VERSION=4.5.0
SOLR_URL=https://archive.apache.org/dist/lucene/solr/$(SOLR_VERSION)/solr-$(SOLR_VERSION).tgz
ifdef ORACLE
  JDBC_DRIVER=$(JDBC_DRIVER_ORA)
  JDBC_DRIVER_PATH=$(JDBC_DRIVER_ORA_PATH)
  DBSUBDIR=oracle
else
  JDBC_DRIVER=$(JDBC_DRIVER_PSQL)
  JDBC_DRIVER_PATH=$(JDBC_DRIVER_PSQL_PATH)
  DBSUBDIR=postgres
endif

solr-$(SOLR_VERSION).tgz.asc:
	curl -f "$(SOLR_URL).asc" -o "$@"

solr-$(SOLR_VERSION).tgz: solr-$(SOLR_VERSION).tgz.asc
	curl -f "$(SOLR_URL)" -o "$@"

verify: solr-$(SOLR_VERSION).tgz solr-$(SOLR_VERSION).tgz.asc
	gpg2 --import APACHE_KEYS
	gpg2 -v --verify "$<.asc" "$<"

ROOT_FILES=contrib dist
EXAMPLE_FILES=etc contexts lib logs resources solr-webapp \
			  webapps README.txt start.jar

$(ROOT_FILES): solr-$(SOLR_VERSION).tgz
	$(TAR_COMMAND) xzf $< --touch --strip-components=1 solr-$(SOLR_VERSION)/$@

$(EXAMPLE_FILES): solr-$(SOLR_VERSION).tgz
	$(TAR_COMMAND) xzf $< --touch --strip-components=2 solr-$(SOLR_VERSION)/example/$@

DRIVER=contrib/dataimporthandler/lib/$(JDBC_DRIVER)
$(DRIVER): $(JDBC_DRIVER_PATH)
	test -d contrib || $(MAKE) contrib
	cp --reflink=auto "$<" "$@"

solr: solr-$(SOLR_VERSION).tgz
	$(TAR_COMMAND) xzf $< --touch --strip-components=2 \
		--exclude $(SOLR_VERSION)/solr/collection1 \
		solr-$(SOLR_VERSION)/example/solr

solr/collection1:
	test -d solr || $(MAKE) solr #don't add dep because solr is touched on each run
	$(TAR_COMMAND) xzf solr-$(SOLR_VERSION).tgz --touch --strip-components=3 \
		-C solr solr-$(SOLR_VERSION)/example/solr/collection1
	rm $@/core.properties #so solr doesn't see the dir as core

CORE_TARGETS=solr/<core_name> solr/<core_name>/conf/schema.xml
DATA_CONFIG_CORE_TARGETS=solr/<core_name>/conf/data-config.xml
FOUND_CORES=$(patsubst schemas/schema_%.xml,\
            %,\
            $(wildcard schemas/schema_*.xml))
FOUND_CORE_TARGETS=$(foreach corename,$(FOUND_CORES),\
        $(subst <core_name>,$(corename),$(CORE_TARGETS)))

FOUND_DATA_CONFIG_CORES=$(patsubst data-config/$(DBSUBDIR)/data-config_%.xml.php,\
            %,\
            $(wildcard data-config/$(DBSUBDIR)/data-config_*.xml.php))
FOUND_DATA_CONFIG_CORE_TARGETS=$(foreach corename,$(FOUND_DATA_CONFIG_CORES),\
        $(subst <core_name>,$(corename),$(DATA_CONFIG_CORE_TARGETS)))
#$(info targets: $(FOUND_CORE_TARGETS))
FOUND_CORE_DIRS=$(foreach corename,$(FOUND_CORES),solr/$(corename))
FULL_IMPORT_TARGETS=$(foreach corename,$(FOUND_DATA_CONFIG_CORES),$(corename)_full_import)
DELTA_IMPORT_TARGETS=$(foreach corename,$(FOUND_DATA_CONFIG_CORES),$(corename)_delta_import)

$(FOUND_CORE_DIRS): solr/collection1 solrconfig.patch
	rsync -a $</* $@
	echo "name=$(notdir $@)" > $@/core.properties
	rm $@/conf/schema.xml
	cd $@/conf && patch -p0 < $(realpath solrconfig.patch)

solr/%/conf/schema.xml: solr/% schemas/schema_%.xml
	test -d solr/$* || $(MAKE) solr/$* #solr/$* is touched on each solr run
	cp schemas/schema_$*.xml $@

solr/%/conf/data-config.xml: data-config/$(DBSUBDIR)/data-config_%.xml.php
	test -d solr/$* || $(MAKE) solr/$* #solr/$* is touched on each solr run
	php -d variables_order=E $< > $@

solr_home: verify $(EXAMPLE_FILES) $(DRIVER) $(FOUND_CORE_TARGETS) $(FOUND_DATA_CONFIG_CORE_TARGETS) $(ROOT_FILES)

$(FULL_IMPORT_TARGETS):
	curl -f "http://localhost:8983/solr/$(patsubst %_full_import,%,$@)/dataimport?command=full-import"

$(DELTA_IMPORT_TARGETS):
	curl -f "http://localhost:8983/solr/$(patsubst %_delta_import,%,$@)/dataimport?command=delta-import"

clean:
	rm -rf $(EXAMPLE_FILES) $(ROOT_FILES) solr solr-$(SOLR_VERSION).tgz

clean_cores:
	find solr -maxdepth 1 -mindepth 1 -type d ! -name bin ! -name collection1 -exec rm -r {} \;

.PHONY: start solr_home clean solr/%/conf/data-config.xml verify
