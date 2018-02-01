--
-- Name: ctd_td_inclusion_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_inclusion_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.disease_severity, v.fev1) AS id,
    v.ref_article_protocol_id,
    v.trial_age,
    v.disease_severity,
    v.difficult_to_treat,
    v.asthma_diagnosis,
    v.inhaled_steroid_dose,
    v.laba,
    v.ocs,
    v.xolair,
    v.ltra_inhibitors,
    v.asthma_phenotype,
    v.fev1,
    v.fev1_reversibility,
    v.tlc,
    v.fev1_fvc,
    v.fvc,
    v.dlco,
    v.sgrq,
    v.hrct,
    v.biopsy,
    v.dyspnea_on_exertion,
    v.concomitant_med
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.trial_age,
            ctd_full.disease_severity,
            ctd_full.difficult_to_treat,
            ctd_full.asthma_diagnosis,
            ctd_full.inhaled_steroid_dose,
            ctd_full.laba,
            ctd_full.ocs,
            ctd_full.xolair,
            ctd_full.ltra_inhibitors,
            ctd_full.asthma_phenotype,
            ctd_full.fev1,
            ctd_full.fev1_reversibility,
            ctd_full.tlc,
            ctd_full.fev1_fvc,
            ctd_full.fvc,
            ctd_full.dlco,
            ctd_full.sgrq,
            ctd_full.hrct,
            ctd_full.biopsy,
            ctd_full.dyspnea_on_exertion,
            ctd_full.concomitant_med
           FROM ctd_full
          WHERE (((ctd_full.fev1 IS NOT NULL) AND ((ctd_full.fev1)::text <> ''::text)) OR ((ctd_full.disease_severity IS NOT NULL) AND ((ctd_full.disease_severity)::text <> ''::text)) OR ((ctd_full.trial_age IS NOT NULL) AND ((ctd_full.trial_age)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.disease_severity, ctd_full.fev1) v;

