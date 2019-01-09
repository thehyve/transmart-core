SUBDIRS := $(wildcard */.)

TRANSMART_COPY_VERSION := 17.1-HYVE-5.7
TRANSMART_COPY := transmart-copy-$(TRANSMART_COPY_VERSION).jar
TRANSMART_COPY_SRC_URL := https://repo.thehyve.nl/content/repositories/releases/org/transmartproject/transmart-copy/$(TRANSMART_COPY_VERSION)/transmart-copy-$(TRANSMART_COPY_VERSION).jar

$(TRANSMART_COPY):
	curl -f $(TRANSMART_COPY_SRC_URL) > $@

load: $(SUBDIRS)
$(SUBDIRS): $(TRANSMART_COPY)
	@echo "Loading test study from $@ folder."
	@PGUSER=tm_cz PGPASSWORD=${TM_CZ_PWD} java -jar $(TRANSMART_COPY) -d $@

.PHONY: load $(SUBDIRS)
