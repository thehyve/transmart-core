--
-- Name: ctd_expr_source_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_expr_source_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.expr_chg_source_type) AS id,
    v.ref_article_protocol_id,
    v.expr_chg_source_type,
    v.expr_chg_technique,
    v.expr_chg_description
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.expr_chg_source_type,
            ctd_full.expr_chg_technique,
            ctd_full.expr_chg_description
           FROM ctd_full
          WHERE (((ctd_full.expr_chg_source_type IS NOT NULL) AND ((ctd_full.expr_chg_source_type)::text <> ''::text)) OR ((ctd_full.expr_chg_description IS NOT NULL) AND ((ctd_full.expr_chg_description)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.expr_chg_source_type) v;

