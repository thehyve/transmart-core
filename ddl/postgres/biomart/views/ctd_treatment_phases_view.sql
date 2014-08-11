--
-- Name: ctd_treatment_phases_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_treatment_phases_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.trtmt_description, v.trtmt_ocs) AS id, v.ref_article_protocol_id, v.trtmt_ocs, v.trtmt_ics, v.trtmt_laba, v.trtmt_ltra, v.trtmt_corticosteroids, v.trtmt_anti_fibrotics, v.trtmt_immunosuppressive, v.trtmt_cytotoxic, v.trtmt_description FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.trtmt_ocs, ctd_full.trtmt_ics, ctd_full.trtmt_laba, ctd_full.trtmt_ltra, ctd_full.trtmt_corticosteroids, ctd_full.trtmt_anti_fibrotics, ctd_full.trtmt_immunosuppressive, ctd_full.trtmt_cytotoxic, ctd_full.trtmt_description FROM ctd_full WHERE ((((ctd_full.trtmt_ocs IS NOT NULL) AND ((ctd_full.trtmt_ocs)::text <> ''::text)) OR ((ctd_full.trtmt_description IS NOT NULL) AND ((ctd_full.trtmt_description)::text <> ''::text))) OR ((ctd_full.trtmt_immunosuppressive IS NOT NULL) AND ((ctd_full.trtmt_immunosuppressive)::text <> ''::text))) ORDER BY ctd_full.ref_article_protocol_id, ctd_full.trtmt_description, ctd_full.trtmt_ocs) v;

