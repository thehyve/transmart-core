--
-- Name: de_variant_metadata_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_variant_metadata_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_variant_metadata; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_variant_metadata (
    de_variant_metadata_id integer DEFAULT nextval('de_variant_metadata_seq'::regclass) NOT NULL,
    dataset_id character varying(50),
    key character varying(255),
    value text
);

--
-- Name: de_variant_metadata de_variant_metadata_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_metadata
    ADD CONSTRAINT de_variant_metadata_pk PRIMARY KEY (de_variant_metadata_id);

--
-- Name: de_variant_metadata var_met_dataset_id; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_metadata
    ADD CONSTRAINT var_met_dataset_id FOREIGN KEY (dataset_id) REFERENCES de_variant_dataset(dataset_id);

