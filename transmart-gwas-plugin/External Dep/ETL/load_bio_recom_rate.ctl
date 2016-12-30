
LOAD DATA
INFILE 'recombination_rate_map_all.txt'
   INTO TABLE bio_recombination_rates
   FIELDS TERMINATED BY X'9'
   TRAILING NULLCOLS
   (
    chromosome,
    position,
    rate,
    map
   )
