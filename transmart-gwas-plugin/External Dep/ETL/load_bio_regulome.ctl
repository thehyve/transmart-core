
LOAD DATA
INFILE 'regulome_score_all.txt'
   INTO TABLE bio_regulome_score
   FIELDS TERMINATED BY X'9'
   TRAILING NULLCOLS
   (
    chromosome,
    position,
    rs_id,
    score
   )
