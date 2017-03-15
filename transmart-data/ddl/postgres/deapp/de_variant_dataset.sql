--
-- Name: de_variant_dataset; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_variant_dataset (
    dataset_id character varying(50) NOT NULL,
    datasource_id character varying(200),
    etl_id character varying(20),
    etl_date date,
    genome character varying(50) NOT NULL,
    metadata_comment text,
    variant_dataset_type character varying(50),
    gpl_id character varying(50)
);

--
-- Name: de_variant_dataset dataset_id; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_dataset
    ADD CONSTRAINT dataset_id PRIMARY KEY (dataset_id);

--
-- Name: de_variant_dataset gpl_id; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_dataset
    ADD CONSTRAINT gpl_id FOREIGN KEY (gpl_id) REFERENCES de_gpl_info(platform);

