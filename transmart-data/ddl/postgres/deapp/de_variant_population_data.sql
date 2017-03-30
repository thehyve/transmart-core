--
-- Name: de_variant_population_data_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_variant_population_data_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_variant_population_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_variant_population_data (
    variant_population_data_id bigint DEFAULT nextval('de_variant_population_data_seq'::regclass) NOT NULL,
    dataset_id character varying(50),
    chr character varying(50),
    pos bigint,
    info_name character varying(100),
    info_index integer DEFAULT 0,
    integer_value bigint,
    float_value double precision,
    text_value character varying(4000)
);

--
-- Name: de_variant_population_data de_variant_population_data_id_idx; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_population_data
    ADD CONSTRAINT de_variant_population_data_id_idx PRIMARY KEY (variant_population_data_id);

--
-- Name: de_variant_population_data_default_idx; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX de_variant_population_data_default_idx ON de_variant_population_data USING btree (dataset_id, chr, pos, info_name);

--
-- Name: de_variant_population_data de_variant_population_data_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_population_data
    ADD CONSTRAINT de_variant_population_data_fk FOREIGN KEY (dataset_id) REFERENCES de_variant_dataset(dataset_id);

