--
-- Name: ctd_td_smoker_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_smoker_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.trial_smokers_pct) AS id,
    v.ref_article_protocol_id,
    v.trial_smokers_pct,
    v.trial_former_smokers_pct,
    v.trial_never_smokers_pct,
    v.trial_pack_years
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.trial_smokers_pct,
            ctd_full.trial_former_smokers_pct,
            ctd_full.trial_never_smokers_pct,
            ctd_full.trial_pack_years
           FROM ctd_full
          WHERE (((ctd_full.trial_smokers_pct IS NOT NULL) AND ((ctd_full.trial_smokers_pct)::text <> ''::text)) OR ((ctd_full.trial_never_smokers_pct IS NOT NULL) AND ((ctd_full.trial_never_smokers_pct)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.trial_smokers_pct) v;

