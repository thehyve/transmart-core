--
-- Name: ctd_td_excl_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_td_excl_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id) AS id,
    v.ref_article_protocol_id,
    v.exclusion_criteria1,
    v.exclusion_criteria2,
    v.minimal_symptoms,
    v.rescue_medication_use,
    v.control_details,
    v.blinding_procedure,
    v.number_of_arms,
    v.description1,
    v.description2
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            "substring"(ctd_full.exclusion_criteria, 1, 4000) AS exclusion_criteria1,
            "substring"(ctd_full.exclusion_criteria, 4001, 2000) AS exclusion_criteria2,
            ctd_full.minimal_symptoms,
            ctd_full.rescue_medication_use,
            ctd_full.control_details,
            ctd_full.blinding_procedure,
            ctd_full.number_of_arms,
            "substring"(ctd_full.description, 1, 4000) AS description1,
            "substring"(ctd_full.description, 4001, 2000) AS description2
           FROM ctd_full
          WHERE (((ctd_full.blinding_procedure IS NOT NULL) AND ((ctd_full.blinding_procedure)::text <> ''::text)) OR ((ctd_full.number_of_arms IS NOT NULL) AND ((ctd_full.number_of_arms)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id) v;

