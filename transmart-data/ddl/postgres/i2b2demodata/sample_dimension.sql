--
-- Name: sample_dimension; Type: TABLE; Schema: i2b2demodata; Owner: -
--
CREATE TABLE sample_dimension (
    sample_cd character varying(200) NOT NULL
);

--
-- Name: sample_dimension_pk; Type: CONSTRAINT; Schema: i2b2demodata; Owner: -
--
ALTER TABLE ONLY sample_dimension
    ADD CONSTRAINT sample_dimension_pk PRIMARY KEY (sample_cd);

