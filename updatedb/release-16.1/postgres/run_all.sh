#!/bin/sh -f

./update-00-check.sh

psql -f ./update-11-amapp-trigger.sql

psql -f ./update-21-biomart-indexname.sql
psql -f ./update-22-biomart-key.sql
psql -f ./update-23-biomart-newtable.sql
psql -f ./update-24-biomart-coltype.sql
psql -f ./update-25-biomart-owner-gwas.sql

psql -f ./update-31a-deapp-newtable-geneinfo.sql
psql -f ./update-31b-deapp-newtable-genesource.sql
psql -f ./update-32-deapp-snpgenemap_view.sql
psql -f ./update-33-deapp-triggerfunction.sql
psql -f ./update-34a-deapp-newcol-gplinfo.sql
psql -f ./update-34b-deapp-newcol-rnaseq.sql
psql -f ./update-35-deapp-annotation-type.sql
psql -f ./update-36a-deapp-proteomics-sequence.sql
psql -f ./update-36b-deapp-proteomics-partitionseq.sql
psql -f ./update-37-deapp-rcsnpinfo-indexes.sql
psql -f ./update-38a-deapp-dropindex.sql
psql -f ./update-38b-deapp-rcsnpinfo-addcolumn.sql
psql -f ./update-38c-deapp-rcsnpinfo1-droptable.sql
psql -f ./update-39-deapp-snpdataprobe-triggername.sql 

psql -f ./update-41a-galaxy-grants.sql
psql -f ./update-41b-galaxy-newseq-hibernate.sql
psql -f ./update-41c-galaxy-newseq-status.sql


psql -f ./update-51-i2b2demodata-trigger.sql
psql -f ./update-52-i2b2demodata-asyncjob-column.sql

psql -f ./update-61-i2b2metadata-i2b2-seq.sql

psql -f ./update-72a-searchapp-newtable-formlayout.sql
psql -f ./update-72b-searchapp-newtable-upload.sql
psql -c "COPY searchapp.search_form_layout FROM STDIN CSV DELIMITER E'\\t'"  < ../../../data/common/searchapp/search_form_layout.tsv
psql -f ./update-73-searchapp-seqcache.sql
psql -f ./update-74-searchapp-changepwd.sql


psql -f ./update-81a-tmcz-function-createsec.sql
psql -f ./update-81b-tmcz-funcmod-chromregion.sql
psql -f ./update-81c-tmcz-function-incclinical.sql
psql -f ./update-82a-tmcz-droptable-formlayout.sql
psql -f ./update-83a-tmcz-funcmod-backouttrial.sql
psql -f ./update-83b-tmcz-funcmod-annodeapp.sql
psql -f ./update-83c-tmcz-funcmod-clinical.sql
psql -f ./update-83d-tmcz-funcmod-annometab.sql
psql -f ./update-83e-tmcz-funcmod-annoprot.sql
psql -f ./update-83f-tmcz-funcmod-loadrbm.sql
psql -f ./update-83g-tmcz-funcmod-zscoremetab.sql
psql -f ./update-83h-tmcz-funcmod-dataacgh.sql
psql -f ./update-83i-tmcz-funcmod-datametab.sql
psql -f ./update-83j-tmcz-funcmod-datamrna.sql
psql -f ./update-83k-tmcz-funcmod-dataprot.sql
psql -f ./update-83l-tmcz-funcmod-datarnaseq.sql
psql -f ./update-83m-tmcz-funcmod-datarnaseq2.sql

psql -f ./update-84a-tmlz-colwidth-subj.sql
psql -f ./update-84b-tmlz-colwidth-desc.sql

psql -f ./update-85a-tmwz-colwidth-subj.sql

psql -f ./update-91a-tsbatch-role.sql
psql -f ./update-91b-tsbatch-modrole.sql

./update-92-tsbatch-make.sh

psql -f ./update-93-tsbatch-tablespace.sql
