R_MIRROR = http://cran-mirror.cs.uu.nl
R_VERSION = 3.0.1
R_SOURCE = R-$(R_VERSION)
PREFIX=root
R_EXEC=$(PREFIX)/bin/R

$(R_SOURCE):
	curl -f $(R_MIRROR)/src/base/R-3/$@.tar.gz | tar xzf -

build/Makefile: $(R_SOURCE)
	mkdir -p build && cd build && \
		CFLAGS="-O2 -march=native -mtune=native" \
		CXXFLAGS="-O2 -march=native -mtune=native" \
		../$(R_SOURCE)/configure --prefix=$(realpath .)/$(PREFIX) \
		--enable-R-shlib --without-readline \
		--with-libpng --without-jpeglib --without-libtiff \
		--without-system-xz --without-ICU \
		--without-recommended-packages

$(R_EXEC): build/Makefile
	 $(MAKE) -C build -j8 && $(MAKE) -C build -j8 install

cran_packages: cran_pkg.R $(R_EXEC)
	CRAN_MIRROR=$(R_MIRROR) $(R_EXEC) -f $<

other_packages: other_pkg.R cran_packages $(R_EXEC)
	$(R_EXEC) -f $<

install_packages: cran_packages other_packages

start_Rserve start_Rserve.dbg:
	$(R_EXEC) CMD $(patsubst start_%,%,$@)

# must be run as root
install_rserve_init:
	TRANSMART_USER=$(TRANSMART_USER) php -d variables_order=E -d open_basedir=/ \
		rserve.php > /etc/init.d/rserve
	chmod +x /etc/init.d/rserve

clean:
	rm -rf build $(R_SOURCE) root

.PHONY: install_packages cran_packages other_packages clean \
		start_Rserve start_Rserve.dbg install_init

.DELETE_ON_ERROR:
