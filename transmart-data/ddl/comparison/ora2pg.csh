#!/bin/bash

type=$1
types=$1"s"


ora2pg -u amapp -w amapp --type $type --out $types-amapp.sql
ora2pg -u biomart -w biomart --type $type --out $types-biomart.sql
ora2pg -u biomart_user -w biomart_user --type $type --out $types-biomart_user.sql
ora2pg -u deapp -w deapp --type $type --out $types-deapp.sql
ora2pg -u fmapp -w fmapp --type $type --out $types-fmapp.sql
##ora2pg -u galaxy -w galaxy --type $type --out $types-galaxy.sql
##ora2pg -u i2b2demo -w i2b2demo --type $type --out $types-i2b2demo.sql
##ora2pg -u i2b2meta -w i2b2meta --type $type --out $types-i2b2meta.sql
ora2pg -u searchapp -w searchapp --type $type --out $types-searchapp.sql
ora2pg -u tm_cz -w tm_cz --type $type --out $types-tm_cz.sql
ora2pg -u tm_lz -w tm_lz --type $type --out $types-tm_lz.sql
ora2pg -u tm_wz -w tm_wz --type $type --out $types-tm_wz.sql

