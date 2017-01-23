--
-- Name: search_user_feedback; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_user_feedback (
    search_user_feedback_id bigint NOT NULL,
    search_user_id bigint,
    create_date timestamp without time zone,
    feedback_text character varying(2000),
    app_version character varying(100)
);

--
-- Name: rdt_search_user_fdbk_pk; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_user_feedback
    ADD CONSTRAINT rdt_search_user_fdbk_pk PRIMARY KEY (search_user_feedback_id);

