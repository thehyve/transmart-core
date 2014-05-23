--
-- Name: ctd_events_view; Type: VIEW; Schema: biomart; Owner: -
--
CREATE VIEW ctd_events_view AS
 SELECT row_number() OVER (ORDER BY v.ref_article_protocol_id, v.definition_of_the_event) AS id,
    v.ref_article_protocol_id,
    v.definition_of_the_event,
    v.number_of_events,
    v.event_rate,
    v.time_to_event,
    v.event_pct_reduction,
    v.event_p_value,
    v.event_description
   FROM ( SELECT DISTINCT ctd_full.ref_article_protocol_id,
            ctd_full.definition_of_the_event,
            ctd_full.number_of_events,
            ctd_full.event_rate,
            ctd_full.time_to_event,
            ctd_full.event_pct_reduction,
            ctd_full.event_p_value,
            ctd_full.event_description
           FROM ctd_full
          WHERE (((ctd_full.definition_of_the_event IS NOT NULL) AND ((ctd_full.definition_of_the_event)::text <> ''::text)) OR ((ctd_full.event_description IS NOT NULL) AND ((ctd_full.event_description)::text <> ''::text)))
          ORDER BY ctd_full.ref_article_protocol_id, ctd_full.definition_of_the_event) v;

