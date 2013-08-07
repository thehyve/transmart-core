include ../makefile.inc

SOLR_DL=http://psg.mtu.edu/pub/apache/lucene/solr
JDBC_DL=http://jdbc.postgresql.org/download
SOLR_VERSION=4.4.0
JDBC_DRIVER=postgresql-9.2-1003.jdbc4.jar

solr-$(SOLR_VERSION).tgz:
	curl -f '$(SOLR_DL)/$(SOLR_VERSION)/solr-$(SOLR_VERSION).tgz' -o $@

ROOT_FILES=contrib dist
EXAMPLE_FILES=etc contexts lib logs resources solr-webapp \
			  webapps README.txt start.jar

$(ROOT_FILES): solr-$(SOLR_VERSION).tgz
	tar xzf $< --touch --strip-components=1 solr-$(SOLR_VERSION)/$@

$(EXAMPLE_FILES): solr-$(SOLR_VERSION).tgz
	tar xzf $< --touch --strip-components=2 solr-$(SOLR_VERSION)/example/$@

DRIVER=contrib/dataimporthandler/lib/$(JDBC_DRIVER)
$(DRIVER):
	test -d contrib || $(MAKE) contrib
	curl -f '$(JDBC_DL)/$(JDBC_DRIVER)' -o $@

solr: solr-$(SOLR_VERSION).tgz
	tar xzf $< --touch --strip-components=2 \
		--exclude $(SOLR_VERSION)/solr/collection1 \
		solr-$(SOLR_VERSION)/example/solr

solr/collection1:
	test -d solr || $(MAKE) solr #don't add dep because solr is touched on each run
	tar xzf solr-$(SOLR_VERSION).tgz --touch --strip-components=3 \
		-C solr solr-$(SOLR_VERSION)/example/solr/collection1
	rm $@/core.properties #so solr doesn't see the dir as core

CORE_TARGETS=solr/<core_name> solr/<core_name>/conf/schema.xml solr/<core_name>/conf/data-config.xml
RWG_CORE_TARGETS=$(subst <core_name>,rwg,$(CORE_TARGETS))
SAMPLE_CORE_TARGETS=$(subst <core_name>,sample,$(CORE_TARGETS))

solr/rwg solr/sample: solr/collection1 dataimport.patch
	rsync -a $</* $@
	echo "name=$(notdir $@)" > $@/core.properties
	rm $@/conf/schema.xml
	cd $@/conf && patch -p0 < $(realpath dataimport.patch)

solr/%/conf/schema.xml: solr/% schemas/schema_%.xml
	test -d solr/$* || $(MAKE) solr/$* #solr/$* is touched on each solr run
	cp schemas/schema_$*.xml $@

solr/%/conf/data-config.xml: data-config/postgres/data-config_%.xml.php
	test -d solr/$* || $(MAKE) solr/$* #solr/$* is touched on each solr run
	php -d variables_order=E $< > $@

start: $(EXAMPLE_FILES) $(DRIVER) $(RWG_CORE_TARGETS) $(SAMPLE_CORE_TARGETS) $(ROOT_FILES)
	java -jar start.jar

rwg_full_import sample_full_import:
	curl -f "http://localhost:8983/solr/$(patsubst %_full_import,%,$@)/dataimport?command=full-import"

rwg_delta_import sample_delta_import:
	curl -f "http://localhost:8983/solr/$(patsubst %_delta_import,%,$@)/dataimport?command=delta-import"

clean:
	rm -rf $(EXAMPLE_FILES) $(ROOT_FILES) solr solr-$(SOLR_VERSION).tgz

.PHONY: start clean
