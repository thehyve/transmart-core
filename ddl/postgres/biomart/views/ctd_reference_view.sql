--
-- Name: ctd_reference_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_reference_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.ref_record_id) AS id,
    v.ref_article_protocol_id,
    v.ref_article_pmid,
    v.ref_protocol_id,
    v.ref_title,
    v.ref_record_id,
    v.ref_back_reference
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.ref_article_pmid,
            ctd_full.ref_protocol_id,
            ctd_full.ref_title,
            ctd_full.ref_record_id,
            ctd_full.ref_back_reference
           FROM ctd_full
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.ref_record_id) v;

