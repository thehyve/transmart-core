create or replace force view "BIOMART"."VW_FACETED_SEARCH_DISEASE"
("BIO_ASSAY_ANALYSIS_ID","SOLR_HIERARCHY") as
select z.bio_assay_analysis_id
,replace(trim(leading '/' from solr_hierarchy),'//','/') as solr_hierarchy
from (
select y.bio_assay_analysis_id
,listagg(y.path,'/') within group (order by y.path) as solr_hierarchy
from
(select x.bio_assay_analysis_id, x.top_node, max(x.path) as path
from (
select distinct bdd.bio_data_id as bio_assay_analysis_id
,substr(mp.path,2,11) as top_node
,mp.path as path
from biomart.bio_data_disease bdd
,biomart.bio_disease bd
,(select mc.ui as unique_id, mc.mh as mesh_name, mc.mn as child_number
,case when instr(mc.mn,'.') = 0 then null else substr(mc.mn,1,instr(mc.mn,'.',-1)-1) end as parent_number
,SYS_CONNECT_BY_PATH('DIS:' || mc.ui, '/') as path
from biomart.mesh mc
start with instr(mc.mn,'.') = 0
connect by prior mc.mn = case when instr(mc.mn,'.') = 0 then null else substr(mc.mn,1,instr(mc.mn,'.',-1)-1) end) mc
,(select mc.ui as unique_id, mc.mh as mesh_name, mc.mn as child_number
,case when instr(mc.mn,'.') = 0 then null else substr(mc.mn,1,instr(mc.mn,'.',-1)-1) end as parent_number
,SYS_CONNECT_BY_PATH('DIS:' || mc.ui, '/') as path
from biomart.mesh mc
start with instr(mc.mn,'.') = 0
connect by prior mc.mn = case when instr(mc.mn,'.') = 0 then null else substr(mc.mn,1,instr(mc.mn,'.',-1)-1) end) mp
where bdd.bio_disease_id = bd.bio_disease_id
--and bdd.etl_source like 'TEST%'
and bd.mesh_code = mc.unique_id
and mc.child_number like mp.child_number || '%' ) x
group by x.bio_assay_analysis_id, x.top_node
order by x.bio_assay_analysis_id) y
group by y.bio_assay_analysis_id) z
order by bio_assay_analysis_id;