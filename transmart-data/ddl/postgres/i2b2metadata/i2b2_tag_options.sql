--
-- Name: i2b2_tag_options_tag_option_id_seq; Type: SEQUENCE; Schema: i2b2metadata; Owner: -
--
CREATE SEQUENCE i2b2metadata.i2b2_tag_options_tag_option_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: i2b2_tag_options; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE i2b2metadata.i2b2_tag_options (
    tag_option_id integer NOT NULL,
    tag_type_id bigint NOT NULL,
    value character varying(1000)
);

--
-- Name: tag_option_id; Type: DEFAULT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY i2b2metadata.i2b2_tag_options ALTER COLUMN tag_option_id
    SET DEFAULT nextval('i2b2_tag_options_tag_option_id_seq'::regclass);

--
-- Name: i2b2_tag_options_pkey; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY i2b2metadata.i2b2_tag_options
    ADD CONSTRAINT i2b2_tag_options_pkey PRIMARY KEY (tag_option_id);

--
-- Name: idx_i2b2_tag_option_pk; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE UNIQUE INDEX idx_i2b2_tag_option_pk ON i2b2metadata.i2b2_tag_options USING btree (tag_option_id);

--
-- Name: i2b2_tag_options_tag_type_fk; Type: FK CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY i2b2metadata.i2b2_tag_options
    ADD CONSTRAINT i2b2_tag_options_tag_type_fk FOREIGN KEY (tag_type_id)
    REFERENCES i2b2metadata.i2b2_tag_types(tag_type_id);
