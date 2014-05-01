REMOTE_FILE_STORAGE = http://files.thehyve.net
KETTLE_VERSION=4.4.0

UBUNTU_PACKAGES=postgresql make git rsync libcairo-dev php5-cli php5-json curl \
				tar openjdk-7-jdk gfortran g++ unzip

GROOVY_VERSION=2.1.9

ONDREJ_KEY=E5267A6C

GROOVY_ZIP=groovy-binary-$(GROOVY_VERSION).zip
GROOVY_URL=http://dist.groovy.codehaus.org/distributions/$(GROOVY_ZIP)
GROOVY_BIN=groovy-$(GROOVY_VERSION)/bin/groovy

MAVEN_VERSION=3.2.1
MAVEN_ZIP=apache-maven-$(MAVEN_VERSION)-bin.zip
MAVEN_URL=http://apache.mirror.1000mbps.com/maven/maven-3/$(MAVEN_VERSION)/binaries/$(MAVEN_ZIP)
MAVEN_BIN=apache-maven-$(MAVEN_VERSION)/bin/mvn

TRANSMART_LOADER_ARCHIVE = transmart-loader.tar.xz
$(TRANSMART_LOADER_ARCHIVE):
	curl -f $(REMOTE_FILE_STORAGE)/$@ > $@

TRANSMART_LOADER=transmart-loader
$(TRANSMART_LOADER)-from-file-server: $(TRANSMART_LOADER_ARCHIVE)
	mkdir $@ & tar --touch -xf $< -C $@

TRANSMART_LOADER_REPO_GIT_URL = https://github.com/thehyve/transmart-loader.git
update-transmart-loader:
	$(call update_repos,$(TRANSMART_LOADER),$(TRANSMART_LOADER_REPO_GIT_URL),master)

package-transmart-loader: maven
	if [ ! -d $(TRANSMART_LOADER) ]; then \
	git clone -b master '$(TRANSMART_LOADER_REPO_GIT_URL)' $(TRANSMART_LOADER); \
	fi && cd $(TRANSMART_LOADER) && mvn package

$(GROOVY_ZIP):
	curl -f "$(GROOVY_URL)" > $@

$(GROOVY_BIN): $(GROOVY_ZIP)
	unzip $<
	touch $@

groovy: $(GROOVY_BIN)
	ln -s $< $@

$(MAVEN_ZIP):
	curl -f "$(MAVEN_URL)" > $@

$(MAVEN_BIN): $(MAVEN_ZIP)
	unzip $<
	touch $@

mvn: $(MAVEN_BIN)
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
.PHONY: update_repos, update-transmart-loader, package-transmart-loader, $(TRANSMART_LOADER)-from-file-server

KETTLE_ARCHIVE=pdi-ce-$(KETTLE_VERSION)-stable.tar.gz
$(KETTLE_ARCHIVE):
	curl -L -f "http://downloads.sourceforge.net/project/pentaho/Data%20Integration/4.4.0-stable/$@" > $@

data-integration: $(KETTLE_ARCHIVE)
	tar --touch -xzf $<

/var/lib/postgresql/tablespaces:
	/bin/bash -c 'mkdir -p $@/{biomart,deapp,indx,search_app,transmart}'
	chown -R postgres:postgres $@

../vars-ubuntu: vars-ubuntu.php
	php -d variables_order=E $< > $@

ubuntu_deps_root: install_ubuntu_packages /var/lib/postgresql/tablespaces
ubuntu_deps_regular: update_etl data-integration ../vars-ubuntu groovy package-transmart-loader

.DELETE_ON_ERROR:

# vim: ai ts=4 sw=4 noet:
