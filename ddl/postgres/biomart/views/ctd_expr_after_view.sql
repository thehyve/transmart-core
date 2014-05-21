--
-- Name: ctd_expr_after_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_expr_after_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.biomolecule_name) AS id,
    v.ref_article_protocol_id,
    v.biomolecule_name,
    v.expr_after_trtmt_pct,
    v.expr_after_trtmt_number,
    v.expr_aftertrtmt_valuefold_mean,
    v.expr_after_trtmt_sd,
    v.expr_after_trtmt_sem,
    v.expr_after_trtmt_unit
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.biomolecule_name,
            ctd_full.expr_after_trtmt_pct,
            ctd_full.expr_after_trtmt_number,
            ctd_full.expr_aftertrtmt_valuefold_mean,
            ctd_full.expr_after_trtmt_sd,
            ctd_full.expr_after_trtmt_sem,
            ctd_full.expr_after_trtmt_unit
           FROM ctd_full
          WHERE (((ctd_full.biomolecule_name IS NOT NULL) AND ((ctd_full.biomolecule_name)::text <> ''::text)) OR ((ctd_full.expr_aftertrtmt_valuefold_mean IS NOT NULL) AND ((ctd_full.expr_aftertrtmt_valuefold_mean)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.biomolecule_name) v;

