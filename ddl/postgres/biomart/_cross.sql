--
-- Name: bio_metab_subpathway_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW bio_metab_subpathway_view AS
    SELECT sp.id AS subpathway_id, b.bio_marker_id AS asso_bio_marker_id, 'SUBPATHWAY TO METABOLITE'::text AS correl_type FROM (((deapp.de_metabolite_sub_pathways sp JOIN deapp.de_metabolite_sub_pway_metab j ON ((sp.id = j.sub_pathway_id))) JOIN deapp.de_metabolite_annotation m ON ((m.id = j.metabolite_id))) JOIN bio_marker b ON ((((b.bio_marker_type)::text = 'METABOLITE'::text) AND ((b.primary_external_id)::text = (m.hmdb_id)::text))));

--
-- Name: bio_metab_superpathway_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW bio_metab_superpathway_view AS
    SELECT supp.id AS superpathway_id, b.bio_marker_id AS asso_bio_marker_id, 'SUPERPATHWAY TO METABOLITE'::text AS correl_type FROM ((((deapp.de_metabolite_super_pathways supp JOIN deapp.de_metabolite_sub_pathways subp ON ((supp.id = subp.super_pathway_id))) JOIN deapp.de_metabolite_sub_pway_metab j ON ((subp.id = j.sub_pathway_id))) JOIN deapp.de_metabolite_annotation m ON ((m.id = j.metabolite_id))) JOIN bio_marker b ON ((((b.bio_marker_type)::text = 'METABOLITE'::text) AND ((b.primary_external_id)::text = (m.hmdb_id)::text))));

