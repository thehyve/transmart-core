--
-- Name: am_data_uid; Type: TABLE; Schema: amapp; Owner: -
--
CREATE TABLE am_data_uid (
    am_data_id bigint NOT NULL,
    unique_id character varying(300) NOT NULL,
    am_data_type character varying(100) NOT NULL
);

--
-- Name: am_data_uid_pk; Type: CONSTRAINT; Schema: amapp; Owner: -
--
ALTER TABLE ONLY am_data_uid
    ADD CONSTRAINT am_data_uid_pk PRIMARY KEY (am_data_id);

--
-- Name: am_data_uid_uk; Type: CONSTRAINT; Schema: amapp; Owner: -
--
ALTER TABLE ONLY am_data_uid
    ADD CONSTRAINT am_data_uid_uk UNIQUE (unique_id);

