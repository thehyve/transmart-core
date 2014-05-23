--
-- Name: search_user_settings; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_user_settings (
    id bigint NOT NULL,
    setting_name character varying(255) NOT NULL,
    user_id bigint NOT NULL,
    setting_value character varying(255) NOT NULL
);

--
-- Name: search_user_settings_pkey; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_user_settings
    ADD CONSTRAINT search_user_settings_pkey PRIMARY KEY (id);

