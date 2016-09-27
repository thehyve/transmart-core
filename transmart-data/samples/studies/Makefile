include ../../lib/makefile.inc

FEED_LISTS:=public-feeds $(wildcard ../../private-feeds)
datasets: $(FEED_LISTS)
	groovy -cp '$(LIB_DIR)' build_datasets_index.groovy \
		$(foreach f,$(FEED_LISTS),-f $f) -o $@

update_datasets:
	@rm -f datasets
	@$(MAKE) --quiet datasets

# depends on %/%_clinical.tar.xz, but make doesn't support repeated %
%/clinical.params %/clinical:
	$(MAKE) $*/$*_clinical.tar.xz
	tar -C $* -xJf $*/$*_clinical.tar.xz

%/annotation.params %/annotation:
	$(MAKE) $*/$*_annotation.tar.xz
	tar -C $* -xJf $*/$*_annotation.tar.xz

%/ref_annotation.params:
	$(MAKE) $*/$*_ref_annotation.tar.xz
	tar -C $* -xJf $*/$*_ref_annotation.tar.xz

%/expression.params %/expression:
	$(MAKE) $*/$*_expression.tar.xz
	tar -C $* -xJf $*/$*_expression.tar.xz

%/acgh.params %/acgh:
	$(MAKE) $*/$*_acgh.tar.xz
	tar -C $* -xJf $*/$*_acgh.tar.xz

%/mirna.params:
	$(MAKE) $*/$*_mirna.tar.xz
	tar -C $* -xJf $*/$*_mirna.tar.xz

%/mirnaqpcr.params %/mirnaqpcr:
	$(MAKE) $*/$*_mirnaqpcr.tar.xz
	tar -C $* -xJf $*/$*_mirnaqpcr.tar.xz

%/mirnaseq.params %/mirnaseq:
	$(MAKE) $*/$*_mirnaseq.tar.xz
	tar -C $* -xJf $*/$*_mirnaseq.tar.xz

%/proteomics.params:
	$(MAKE) $*/$*_proteomics.tar.xz
	tar -C $* -xJf $*/$*_proteomics.tar.xz

%/msproteomics.params %/msproteomics:
	$(MAKE) $*/$*_msproteomics.tar.xz
	tar -C $* -xJf $*/$*_msproteomics.tar.xz

%/metabolomics.params %/metabolomics:
	$(MAKE) $*/$*_metabolomics.tar.xz
	tar -C $* -xJf $*/$*_metabolomics.tar.xz

%/rbm.params %/rbm:
	$(MAKE) $*/$*_rbm.tar.xz
	tar -C $* -xJf $*/$*_rbm.tar.xz

%/rnaseq.params %.rnaseq:
	$(MAKE) $*/$*_rnaseq.tar.xz
	tar -C $* -xJf $*/$*_rnaseq.tar.xz

%/vcf.params:
	$(MAKE) $*/$*_vcf.tar.xz
	tar -C $* -xJf $*/$*_vcf.tar.xz

TARBALLS := $(shell test ! -f datasets || php enumerate.php tarballs)
$(TARBALLS):
	@test -f datasets || $(MAKE) datasets
	mkdir -p "$$(php -r 'preg_match("@^[^/]+@", $$argv[1], $$m); echo $$m[0];' '$@')"
	./download_tarball.sh '$@' $$(php get_file_location.php '$@')

clean:
	rm -f datasets
	find . -path './*' -prune -type d -exec rm -r '{}' \;

.PHONY: update_datasets clean

.DELETE_ON_ERROR:
