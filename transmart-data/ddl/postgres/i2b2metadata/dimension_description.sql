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
    size_cd character varying(255),
    dimension_type character varying(50),
    sort_index integer
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

--
-- add documentation
--
COMMENT ON TABLE i2b2metadata.dimension_description IS 'All supported dimensions and their properties.';

COMMENT ON COLUMN dimension_description.name IS 'The name of the dimension.';
COMMENT ON COLUMN dimension_description.dimension_type IS 'Indicates whether the dimension represents subjects or observation attributes. [SUBJECT, ATTRIBUTE]';
COMMENT ON COLUMN dimension_description.sort_index IS 'Specifies a relative order between dimensions.';
COMMENT ON COLUMN dimension_description.value_type IS 'T for string, N for numeric, B for raw text and D for date values. [T, N, B, D]';
COMMENT ON COLUMN dimension_description.modifier_code IS 'The modifier code if the dimension is a modifier dimension';
COMMENT ON COLUMN dimension_description.size_cd IS 'Indicates the typical size of the dimension. [SMALL, MEDIUM, LARGE]';
COMMENT ON COLUMN dimension_description.density IS 'Indicates the typical density of the dimension. [DENSE, SPARSE]';
COMMENT ON COLUMN dimension_description.packable IS 'Indicates if dimensions values can be packed when serialising. NOT_PACKABLE is a good default. [PACKABLE, NOT_PACKABLE]';
