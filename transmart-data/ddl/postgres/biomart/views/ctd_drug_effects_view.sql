--
-- Name: ctd_drug_effects_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_drug_effects_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.beneficial_effects, v.adverse_effects) AS id,
    v.ref_article_protocol_id,
    v.discontinuation_rate,
    v.response_rate,
    v.downstream_signaling_effects,
    v.beneficial_effects,
    v.adverse_effects,
    v.pk_pd_parameter,
    v.pk_pd_value,
    v.effect_description
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.discontinuation_rate,
            ctd_full.response_rate,
            ctd_full.downstream_signaling_effects,
            ctd_full.beneficial_effects,
            ctd_full.adverse_effects,
            ctd_full.pk_pd_parameter,
            ctd_full.pk_pd_value,
            ctd_full.effect_description
           FROM ctd_full
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.beneficial_effects, ctd_full.adverse_effects) v;

