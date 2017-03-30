--
-- Name: ctd_runin_therapies_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_runin_therapies_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.runin_description) AS id,
    v.ref_article_protocol_id,
    v.runin_ocs,
    v.runin_ics,
    v.runin_laba,
    v.runin_ltra,
    v.runin_corticosteroids,
    v.runin_anti_fibrotics,
    v.runin_immunosuppressive,
    v.runin_cytotoxic,
    v.runin_description
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.runin_ocs,
            ctd_full.runin_ics,
            ctd_full.runin_laba,
            ctd_full.runin_ltra,
            ctd_full.runin_corticosteroids,
            ctd_full.runin_anti_fibrotics,
            ctd_full.runin_immunosuppressive,
            ctd_full.runin_cytotoxic,
            ctd_full.runin_description
           FROM ctd_full
          WHERE (((ctd_full.runin_ocs IS NOT NULL) AND ((ctd_full.runin_ocs)::text <> ''::text)) OR ((ctd_full.runin_description IS NOT NULL) AND ((ctd_full.runin_description)::text <> ''::text)) OR ((ctd_full.runin_immunosuppressive IS NOT NULL) AND ((ctd_full.runin_immunosuppressive)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.runin_description) v;

