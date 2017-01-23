--
-- Name: jms_subscriptions; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE jms_subscriptions (
    clientid character varying(128) NOT NULL,
    subname character varying(128) NOT NULL,
    topic character varying(255) NOT NULL,
    selector character varying(255)
);

--
-- Name: jms_subscriptions_pkey; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY jms_subscriptions
    ADD CONSTRAINT jms_subscriptions_pkey PRIMARY KEY (clientid, subname);

