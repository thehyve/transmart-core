--
-- Name: ctd_drug_inhibitor_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_drug_inhibitor_view AS
    SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.drug_inhibitor_common_name) AS id, v.ref_article_protocol_id, v.drug_inhibitor_common_name, v.drug_inhibitor_standard_name, v.drug_inhibitor_cas_id FROM (SELECT DISTINCT ctd_full.ref_article_protocol_id, ctd_full.drug_inhibitor_common_name, ctd_full.drug_inhibitor_standard_name, ctd_full.drug_inhibitor_cas_id FROM ctd_full ORDER BY ctd_full.ref_article_protocol_id, ctd_full.drug_inhibitor_common_name) v;

