--
-- Name: ctd_expr_baseline_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_expr_baseline_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.biomolecule_name) AS id,
    v.ref_article_protocol_id,
    v.biomolecule_name,
    v.baseline_expr_pct,
    v.baseline_expr_number,
    v.baseline_expr_value_fold_mean,
    v.baseline_expr_sd,
    v.baseline_expr_sem,
    v.baseline_expr_unit
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.biomolecule_name,
            ctd_full.baseline_expr_pct,
            ctd_full.baseline_expr_number,
            ctd_full.baseline_expr_value_fold_mean,
            ctd_full.baseline_expr_sd,
            ctd_full.baseline_expr_sem,
            ctd_full.baseline_expr_unit
           FROM ctd_full
          WHERE (((ctd_full.biomolecule_name IS NOT NULL) AND ((ctd_full.biomolecule_name)::text <> ''::text)) OR ((ctd_full.baseline_expr_value_fold_mean IS NOT NULL) AND ((ctd_full.baseline_expr_value_fold_mean)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.biomolecule_name) v;

