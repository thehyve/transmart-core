drop materialized view deapp.de_snp_info_hg19_mv;
create materialized view deapp.de_snp_info_hg19_mv 
as
select 
rs_id,
chrom,
pos,
gene_name as rsgene,
exon_intron,
recombination_rate,
regulome_score
from deapp.de_rc_snp_info info
where info.hg_version=19;