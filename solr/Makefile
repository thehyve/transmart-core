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

RWG_CORE_TARGETS=solr/rwg solr/rwg/conf/schema.xml solr/rwg/conf/data-config.xml
solr/rwg: solr/collection1 dataimport.patch
	rsync -a $</* $@
	echo "name=$(notdir $@)" > $@/core.properties
	rm $@/conf/schema.xml
	cd $@/conf && patch -p0 < $(realpath dataimport.patch)
	#sed -i 's|\.\./dist|dist|' $@/conf/solrconfig.xml
	#sed -i 's|\.\./contrib|contrib|' $@/conf/solrconfig.xml

solr/rwg/conf/schema.xml: solr/rwg schemas/schema_rwg.xml
	test -d solr/rwg || $(MAKE) solr/rwg #solr/rwg is touched on each solr run
	cp schemas/schema_rwg.xml $@

solr/rwg/conf/data-config.xml: data-config/postgres/data-config_rwg.xml.php
	test -d solr/rwg || $(MAKE) solr/rwg #solr/rwg is touched on each solr run
	php -d variables_order=E $< > $@

start: $(EXAMPLE_FILES) $(DRIVER) $(RWG_CORE_TARGETS) $(ROOT_FILES)
	java -jar start.jar

rwg_full_import:
	curl -f "http://localhost:8983/solr/rwg/dataimport?command=full-import"

clean:
	rm -rf $(EXAMPLE_FILES) $(ROOT_FILES)

.PHONY: start clean
