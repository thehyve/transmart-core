KETTLE_VERSION=4.4.0

UBUNTU_PACKAGES=postgresql make git rsync libcairo-dev php5-cli php5-json curl \
				tar openjdk-7-jdk gfortran g++

OBDREJ_KEY=E5267A6C 
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
.PHONY: update_repos

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
ubuntu_deps_regular: update_etl data-integration ../vars-ubuntu

# vim: ai ts=4 sw=4 noet:
