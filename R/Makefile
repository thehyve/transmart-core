include ../makefile.inc

R_MIRROR ?= http://cran-mirror.cs.uu.nl
R_VERSION = 3.1.2
R_SOURCE = R-$(R_VERSION)
R_PREFIX ?= $(realpath .)/root
R_EXEC = $(R_PREFIX)/bin/R
RSERVE_CONF ?= $(realpath .)/Rserv_nodaemon.conf

R_FLAGS ?= -O2 -march=native

$(R_SOURCE):
	curl -f $(R_MIRROR)/src/base/R-3/$@.tar.gz | $(TAR_COMMAND) xzf -

build/Makefile: $(R_SOURCE)
	mkdir -p build && cd build && \
		CFLAGS="$(R_FLAGS)" \
		CXXFLAGS="$(R_FLAGS)" \
		FFLAGS="$(R_FLAGS)" \
		LIBnn=lib \
		LDFLAGS="-Wl,-rpath=$(R_PREFIX)/lib/R/lib,--enable-new-dtags" \
		../$(R_SOURCE)/configure --prefix=$(R_PREFIX) \
		--enable-R-shlib --with-readline \
		--enable-R-profiling --enable-memory-profiling \
		--with-libpng --without-jpeglib --without-libtiff \
		--without-system-xz --without-ICU \
		--without-recommended-packages

# for the default prefix, expose a relative path target
ifeq ($(R_PREFIX), $(realpath .)/root)
root/bin/R: $(R_EXEC)
endif

$(R_EXEC): build/Makefile
	 $(MAKE) -C build -j8 && $(MAKE) -C build -j8 install

cran_packages: cran_pkg.R $(R_EXEC)
	CRAN_MIRROR=$(R_MIRROR) $(R_EXEC) -f $<

other_packages: other_pkg.R cran_packages $(R_EXEC)
	CRAN_MIRROR=$(R_MIRROR) $(R_EXEC) -f $<

install_packages: cran_packages other_packages

start_Rserve start_Rserve.dbg:
	$(R_EXEC) CMD $(patsubst start_%,%,$@)

# phony target because it depends on value of env var
write_default_rserve:
	@test -n '$(TRANSMART_USER)' || { \
		echo 'Define $$TRANSMART_USER' 2>&1 ; false; }
	echo 'USER=$(TRANSMART_USER)' > /etc/default/rserve

$(RSERVE_CONF): Rserv_nodaemon.conf
	mkdir -p '$(dir $@)'
	cp '$<' '$@'

# must be run as root
install_rserve_init: /etc/init.d/rserve
/etc/init.d/rserve: write_default_rserve $(RSERVE_CONF)
	R_PREFIX='$(R_PREFIX)' RSERVE_CONF=$(RSERVE_CONF) php \
		-d variables_order=E -d open_basedir=/ \
		rserve.php > $@
	chmod +x '$@'

install_rserve_upstart: /etc/init/rserve.conf
/etc/init/rserve.conf: write_default_rserve $(RSERVE_CONF)
	R_PREFIX='$(R_PREFIX)' RSERVE_CONF=$(RSERVE_CONF) php \
		-d variables_order=E -d open_basedir=/ \
		rserve.conf.php > $@
	initctl reload-configuration

install_rserve_unit: /etc/systemd/system/rserve.service
/etc/systemd/system/rserve.service: $(RSERVE_CONF)
	R_PREFIX='$(R_PREFIX)' RSERVE_CONF=$(RSERVE_CONF) \
		TRANSMART_USER=$(TRANSMART_USER) php \
		-d variables_order=E -d open_basedir=/ rserve.service.php > $@
	systemctl daemon-reload

clean:
	rm -rf build $(R_SOURCE) root

.PHONY: install_packages cran_packages other_packages clean \
		start_Rserve start_Rserve.dbg install_init write_default_rserve

.DELETE_ON_ERROR:
