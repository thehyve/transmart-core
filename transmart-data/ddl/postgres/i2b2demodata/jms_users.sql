--
-- Name: jms_users; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE jms_users (
    userid character varying(32) NOT NULL,
    passwd character varying(32) NOT NULL,
    clientid character varying(128)
);

--
-- Name: jms_users_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY jms_users
    ADD CONSTRAINT jms_users_pkey PRIMARY KEY (userid);

