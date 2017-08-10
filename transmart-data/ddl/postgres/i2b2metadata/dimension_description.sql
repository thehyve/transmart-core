--
-- Name: dimension_description_id_seq; Type: SEQUENCE; Schema: i2b2metadata; Owner: -
--
CREATE SEQUENCE dimension_description_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: dimension_description; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE dimension_description (
    id integer NOT NULL,
    density character varying(255),
    modifier_code character varying(255),
    value_type character varying(50),
    name character varying(255) NOT NULL,
    packable character varying(255),
    size_cd character varying(255)
);

--
-- Name: id; Type: DEFAULT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY dimension_description ALTER COLUMN id SET DEFAULT nextval('dimension_description_id_seq'::regclass);

--
-- Name: dimension_description_pkey; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY dimension_description
    ADD CONSTRAINT dimension_description_pkey PRIMARY KEY (id);

--
-- Name: dimension_description_unique_name; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY dimension_description
    ADD CONSTRAINT dimension_description_unique_name UNIQUE (name);

