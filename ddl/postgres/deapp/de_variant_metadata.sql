--
-- Name: de_variant_metadata; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_variant_metadata (
    de_variant_metadata_id integer NOT NULL,
    dataset_id character varying(50),
    key character varying(255),
    value text
);

--
-- Name: de_variant_metadata_pk; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_metadata
    ADD CONSTRAINT de_variant_metadata_pk PRIMARY KEY (de_variant_metadata_id);

--
-- Name: dataset_id; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_metadata
    ADD CONSTRAINT dataset_id FOREIGN KEY (dataset_id) REFERENCES de_variant_dataset(dataset_id);

