GPL154466_family.soft.gz:
	curl -f ftp://ftp.ncbi.nlm.nih.gov/geo/platforms/GPL15nnn/GPL15466/soft/GPL15466_family.soft.gz > $@

%: %.gz
	gzip -d $<

#GSE37276 is the series number common to all the samples
prepare_GPL15466: GPL154466_family.soft split_GPL.sh
	./split_GPL.sh $< GSE37276

.PHONY: prepare_GPL15466

.DELETE_ON_ERROR:
