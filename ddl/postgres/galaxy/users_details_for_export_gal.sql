--
-- Name: users_details_for_export_gal; Type: TABLE; Schema: galaxy; Owner: -
--
CREATE TABLE users_details_for_export_gal (
    id bigint NOT NULL,
    galaxy_key character varying(100) NOT NULL,
    mail_address character varying(200) NOT NULL,
    username character varying(200) NOT NULL
);

--
-- Name: users_details_for_export_g_pk; Type: CONSTRAINT; Schema: galaxy; Owner: -
--
ALTER TABLE ONLY users_details_for_export_gal
    ADD CONSTRAINT users_details_for_export_g_pk PRIMARY KEY (id);

