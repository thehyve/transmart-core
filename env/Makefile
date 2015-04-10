KETTLE_VERSION=4.4.0

UBUNTU_PACKAGES=postgresql make git rsync libcairo-dev php5-cli php5-json curl \
				tar openjdk-7-jdk gfortran g++ unzip libreadline-dev \
				libxt-dev libpango1.0-dev texlive-fonts-recommended tex-gyre \
				fonts-dejavu-core

GROOVY_VERSION=2.1.9

ONDREJ_KEY=E5267A6C

GROOVY_ZIP=groovy-binary-$(GROOVY_VERSION).zip
GROOVY_URL=http://dist.groovy.codehaus.org/distributions/$(GROOVY_ZIP)
GROOVY_BIN=groovy-$(GROOVY_VERSION)/bin/groovy

PHP_VERSION=5.5.13
PHP_TARBALL=php-$(PHP_VERSION).tar.gz
PHP_SRCDIR=php-$(PHP_VERSION)
PHP_URL=http://nl1.php.net/distributions/php-$(PHP_VERSION).tar.gz
PHP_BIN=php5/bin/php

TRANSMART_LOADER=transmart-loader

TRANSMART_BATCH_VERSION=1.0-SNAPSHOT
TRANSMART_BATCH_FILE=transmart-batch-capsule.jar
TRANSMART_BATCH_URL=https://repo.thehyve.nl/content/repositories/snapshots/org/transmartproject/transmart-batch/$(TRANSMART_BATCH_VERSION)/transmart-batch-$(TRANSMART_BATCH_VERSION)-capsule.jar

%/conf/log4j.properties:
	mkdir -p $*/conf \
	&& echo "log4j.rootLogger=INFO, console" > $@ \
	&& echo "log4j.appender.console=org.apache.log4j.ConsoleAppender" >> $@ \
	&& echo "log4j.appender.console.layout=org.apache.log4j.PatternLayout" >> $@ \
	&& echo "log4j.appender.console.layout.ConversionPattern=%-4r [%t] %-5p %c %x - %m%n" >> $@

TRANSMART_LOADER_SNAPSHOT_NAME=loader-jar-with-dependencies.jar
LATEST_TRANSMART_LOADER_SNAPSHOT_URL=https://ci.ctmmtrait.nl/artifact/TM-TLOAD/shared/build-latest/transmart-loader/target/$(TRANSMART_LOADER_SNAPSHOT_NAME)

# only load the snapshot if the current one is 24 hours old (or older)
# use FORCE_TS_LOADER_DOWNLOAD=1 to override
$(TRANSMART_LOADER)-latest-bamboo-snapshot: $(TRANSMART_LOADER)-latest-bamboo-snapshot/conf/log4j.properties
	if test "$$(find $@/$(TRANSMART_LOADER_SNAPSHOT_NAME) -mtime +1 2> /dev/null)" \
		-o "$$FORCE_TS_LOADER_DOWNLOAD" = 1 \
		-o ! -f $@/$(TRANSMART_LOADER_SNAPSHOT_NAME); then \
		curl -L -f $(LATEST_TRANSMART_LOADER_SNAPSHOT_URL) > $@/$(TRANSMART_LOADER_SNAPSHOT_NAME); \
	fi

# do a conditional request here.
# We could also skip if we checked in the last hour or so (see ts-loader) below
$(TRANSMART_BATCH_FILE):
	curl -s -L -f -R -z '$@' -o '$@' '$(TRANSMART_BATCH_URL)'
	@chmod +x '$@'

batchdb-psql.properties: batchdb-psql.properties.php ../vars
	php -d variables_order=E '$<' > '$@'

batchdb-oracle.properties: batchdb-oracle.properties.php ../vars
	php -d variables_order=E '$<' > '$@'

# for redownload
.PHONY: $(TRANSMART_BATCH_FILE)

$(GROOVY_ZIP):
	curl -f "$(GROOVY_URL)" > $@

$(GROOVY_BIN): $(GROOVY_ZIP)
	unzip $<
	touch $@

groovy: $(GROOVY_BIN)
	ln -s $< $@

$(PHP_TARBALL):
	curl -f '$(PHP_URL)' > $@

$(PHP_SRCDIR)/.keep: $(PHP_TARBALL)
	tar -xf '$<'
	touch '$@'

$(PHP_BIN): $(PHP_SRCDIR)/.keep
	cd $(PHP_SRCDIR) && ./configure --prefix='$(realpath .)/php5' --disable-all --enable-json
	$(MAKE) -j8 -C '$(PHP_SRCDIR)' install

php: $(PHP_BIN)
	ln -s $< $@

install_ubuntu_packages:
	. /etc/lsb-release; \
		test $$DISTRIB_RELEASE != '12.04' || \
	    grep ondrej/php5 /etc/apt/sources.list || ( \
				gpg --recv-keys --keyserver keyserver.ubuntu.com $(ONDREJ_KEY) && \
				(gpg -a --export $(ONDREJ_KEY) | apt-key add -) && \
				echo 'deb http://ppa.launchpad.net/ondrej/php5/ubuntu precise main' >> /etc/apt/sources.list)
	apt-get update
	apt-get install -y $(UBUNTU_PACKAGES)

# <directory> <repos> <branch>
update_repos = if [ ! -d $(1) ]; then \
	git clone -b $(3) '$(2)' $(1); \
	else cd $(1) && git pull; fi

update_etl:
	$(call update_repos,tranSMART-ETL,git://github.com/thehyve/tranSMART-ETL.git,master)
.PHONY: update_repos, $(TRANSMART_LOADER)-from-file-server, $(TRANSMART_LOADER)-latest-bamboo-snapshot

KETTLE_ARCHIVE=pdi-ce-$(KETTLE_VERSION)-stable.tar.gz
$(KETTLE_ARCHIVE):
	curl -L -f "http://downloads.sourceforge.net/project/pentaho/Data%20Integration/4.4.0-stable/$@" > $@

data-integration: data-integration/.keep
.PHONY: data-integration
data-integration/.keep: $(KETTLE_ARCHIVE)
	 tar -xzf $<
	 touch '$@'

/var/lib/postgresql/tablespaces:
	/bin/bash -c 'mkdir -p $@/{biomart,deapp,indx,search_app,transmart}'
	chown -R postgres:postgres $@

../vars: vars-ubuntu.php
	php -d variables_order=E $< > $@

ubuntu_deps_root: install_ubuntu_packages /var/lib/postgresql/tablespaces
ubuntu_deps_regular: update_etl data-integration ../vars groovy

.DELETE_ON_ERROR:

# vim: ai ts=4 sw=4 noet:
