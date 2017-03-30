--
-- Name: ctd_expr_bio_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_expr_bio_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.biomolecule_name) AS id,
    v.ref_article_protocol_id,
    v.biomolecule_name,
    v.biomolecule_id,
    v.biomolecule_type,
    v.biomarker,
    v.biomarker_type
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.biomolecule_name,
            ctd_full.biomolecule_id,
            ctd_full.biomolecule_type,
            ctd_full.biomarker,
            ctd_full.biomarker_type
           FROM ctd_full
          WHERE (((ctd_full.biomolecule_name IS NOT NULL) AND ((ctd_full.biomolecule_name)::text <> ''::text)) OR ((ctd_full.biomolecule_id IS NOT NULL) AND ((ctd_full.biomolecule_id)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.biomolecule_name) v;

