--
-- Name: fm_data_uid; Type: TABLE; Schema: fmapp; Owner: -
--
CREATE TABLE fm_data_uid (
    fm_data_id bigint NOT NULL,
    unique_id character varying(300) NOT NULL,
    fm_data_type character varying(100) NOT NULL
);

--
-- Name: fm_data_uid_pk_1; Type: CONSTRAINT; Schema: fmapp; Owner: -
--
ALTER TABLE ONLY fm_data_uid
    ADD CONSTRAINT fm_data_uid_pk_1 PRIMARY KEY (fm_data_id);

--
-- Name: fm_data_uid_uk_1; Type: CONSTRAINT; Schema: fmapp; Owner: -
--
ALTER TABLE ONLY fm_data_uid
    ADD CONSTRAINT fm_data_uid_uk_1 UNIQUE (unique_id);

