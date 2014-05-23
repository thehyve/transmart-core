--
-- Name: jms_roles; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE jms_roles (
    roleid character varying(32) NOT NULL,
    userid character varying(32) NOT NULL
);

--
-- Name: jms_roles_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY jms_roles
    ADD CONSTRAINT jms_roles_pkey PRIMARY KEY (userid, roleid);

