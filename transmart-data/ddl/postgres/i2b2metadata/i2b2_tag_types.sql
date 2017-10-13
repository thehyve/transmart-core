--
-- Name: i2b2_tag_types_tag_type_id_seq; Type: SEQUENCE; Schema: i2b2metadata; Owner: -
--
CREATE SEQUENCE i2b2_tag_types_tag_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: i2b2_tag_types; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE i2b2_tag_types (
    tag_type_id integer NOT NULL,
    tag_type character varying(255) NOT NULL,
    solr_field_name character varying(255),
    node_type character varying(255) NOT NULL,
    value_type character varying(255) NOT NULL,
    shown_if_empty boolean,
    index integer
);

--
-- Name: tag_type_id; Type: DEFAULT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY i2b2_tag_types ALTER COLUMN tag_type_id SET DEFAULT nextval('i2b2_tag_types_tag_type_id_seq'::regclass);

--
-- Name: i2b2_tag_types_node_type_tag_type_unique; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY i2b2_tag_types
    ADD CONSTRAINT i2b2_tag_types_node_type_tag_type_unique UNIQUE (node_type, tag_type);

--
-- Name: i2b2_tag_types_pkey; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY i2b2_tag_types
    ADD CONSTRAINT i2b2_tag_types_pkey PRIMARY KEY (tag_type_id);

--
-- Name: idx_i2b2_tag_type_pk; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE UNIQUE INDEX idx_i2b2_tag_type_pk ON i2b2_tag_types USING btree (tag_type_id);
