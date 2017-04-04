#!/bin/sh -f

PSQL_COMMAND="${PGSQL_BIN}psql -v ON_ERROR_STOP=1 --single-transaction -X"

./update-00-check.sh

$PSQL_COMMAND -f ./update-11-amapp-trigger.sql

$PSQL_COMMAND -f ./update-21-biomart-indexname.sql
$PSQL_COMMAND -f ./update-22-biomart-key.sql
$PSQL_COMMAND -f ./update-23-biomart-newtable.sql
$PSQL_COMMAND -f ./update-24-biomart-coltype.sql
$PSQL_COMMAND -f ./update-25-biomart-owner-gwas.sql

$PSQL_COMMAND -f ./update-31a-deapp-newtable-geneinfo.sql
$PSQL_COMMAND -f ./update-31b-deapp-newtable-genesource.sql
$PSQL_COMMAND -f ./update-32-deapp-snpgenemap_view.sql
$PSQL_COMMAND -f ./update-33-deapp-triggerfunction.sql
$PSQL_COMMAND -f ./update-34a-deapp-newcol-gplinfo.sql
$PSQL_COMMAND -f ./update-34b-deapp-newcol-rnaseq.sql
$PSQL_COMMAND -f ./update-35-deapp-annotation-type.sql
$PSQL_COMMAND -f ./update-36a-deapp-proteomics-sequence.sql
$PSQL_COMMAND -f ./update-36b-deapp-proteomics-partitionseq.sql
$PSQL_COMMAND -f ./update-37-deapp-rcsnpinfo-indexes.sql
$PSQL_COMMAND -f ./update-38a-deapp-dropindex.sql
$PSQL_COMMAND -f ./update-38b-deapp-rcsnpinfo-addcolumn.sql
$PSQL_COMMAND -f ./update-38c-deapp-rcsnpinfo1-droptable.sql
$PSQL_COMMAND -f ./update-39-deapp-snpdataprobe-triggername.sql 

$PSQL_COMMAND -f ./update-41a-galaxy-grants.sql
$PSQL_COMMAND -f ./update-41b-galaxy-newseq-hibernate.sql
$PSQL_COMMAND -f ./update-41c-galaxy-newseq-status.sql


$PSQL_COMMAND -f ./update-51-i2b2demodata-trigger.sql
$PSQL_COMMAND -f ./update-52-i2b2demodata-asyncjob-column.sql

$PSQL_COMMAND -f ./update-61-i2b2metadata-i2b2-seq.sql

$PSQL_COMMAND -f ./update-72a-searchapp-newtable-formlayout.sql
$PSQL_COMMAND -f ./update-72b-searchapp-newtable-upload.sql
$PSQL_COMMAND -c "COPY searchapp.search_form_layout FROM STDIN CSV DELIMITER E'\\t'"  < ../../../data/common/searchapp/search_form_layout.tsv
$PSQL_COMMAND -f ./update-73-searchapp-seqcache.sql
$PSQL_COMMAND -f ./update-74-searchapp-changepwd.sql


$PSQL_COMMAND -f ./update-81a-tmcz-function-createsec.sql
$PSQL_COMMAND -f ./update-81b-tmcz-funcmod-chromregion.sql
$PSQL_COMMAND -f ./update-81c-tmcz-function-incclinical.sql
$PSQL_COMMAND -f ./update-82a-tmcz-droptable-formlayout.sql
$PSQL_COMMAND -f ./update-83a-tmcz-funcmod-backouttrial.sql
$PSQL_COMMAND -f ./update-83b-tmcz-funcmod-annodeapp.sql
$PSQL_COMMAND -f ./update-83c-tmcz-funcmod-clinical.sql
$PSQL_COMMAND -f ./update-83d-tmcz-funcmod-annometab.sql
$PSQL_COMMAND -f ./update-83e-tmcz-funcmod-annoprot.sql
$PSQL_COMMAND -f ./update-83f-tmcz-funcmod-loadrbm.sql
$PSQL_COMMAND -f ./update-83g-tmcz-funcmod-zscoremetab.sql
$PSQL_COMMAND -f ./update-83h-tmcz-funcmod-dataacgh.sql
$PSQL_COMMAND -f ./update-83i-tmcz-funcmod-datametab.sql
$PSQL_COMMAND -f ./update-83j-tmcz-funcmod-datamrna.sql
$PSQL_COMMAND -f ./update-83k-tmcz-funcmod-dataprot.sql
$PSQL_COMMAND -f ./update-83l-tmcz-funcmod-datarnaseq.sql
$PSQL_COMMAND -f ./update-83m-tmcz-funcmod-datarnaseq2.sql

$PSQL_COMMAND -f ./update-84a-tmlz-colwidth-subj.sql
$PSQL_COMMAND -f ./update-84b-tmlz-colwidth-desc.sql

$PSQL_COMMAND -f ./update-85a-tmwz-colwidth-subj.sql

$PSQL_COMMAND -f ./update-91a-tsbatch-role.sql
$PSQL_COMMAND -f ./update-91b-tsbatch-modrole.sql

./update-92-tsbatch-make.sh

$PSQL_COMMAND -f ./update-93-tsbatch-tablespace.sql
