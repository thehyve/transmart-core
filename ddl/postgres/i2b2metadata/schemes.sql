--
-- Name: schemes; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE schemes (
    c_key character varying(50) NOT NULL,
    c_name character varying(50) NOT NULL,
    c_description character varying(100)
);

--
-- Name: schemes_pk; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY schemes
    ADD CONSTRAINT schemes_pk PRIMARY KEY (c_key);

