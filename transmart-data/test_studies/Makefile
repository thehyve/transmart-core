SUBDIRS := $(wildcard */.)

TRANSMART_COPY_VERSION := $(shell cat ../VERSION)
TRANSMART_COPY := transmart-copy-$(TRANSMART_COPY_VERSION).jar

TRANSMART_COPY_VERSION_LC = `echo $(TRANSMART_COPY_VERSION) | tr A-Z a-z`
ifneq (,$(findstring snapshot,$(TRANSMART_COPY_VERSION_LC)))
    REPOSITORY := snapshots
else
    REPOSITORY := releases
endif

TRANSMART_COPY_SRC_URL := https://repo.thehyve.nl/service/local/artifact/maven/redirect?r=${REPOSITORY}&g=org.transmartproject&a=transmart-copy&v=${TRANSMART_COPY_VERSION}&p=jar

$(TRANSMART_COPY):
	curl -s -L -f -R -z '$@' -o '$@' '$(TRANSMART_COPY_SRC_URL)'
	@chmod +x '$@'

load: $(SUBDIRS)
$(SUBDIRS): $(TRANSMART_COPY)
	@echo "Loading test study from $@ folder."
	@PGUSER=tm_cz PGPASSWORD=${TM_CZ_PWD} java -jar $(TRANSMART_COPY) -d $@

.PHONY: load $(SUBDIRS)
