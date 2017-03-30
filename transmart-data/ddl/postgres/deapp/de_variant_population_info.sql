--
-- Name: de_variant_population_info_seq; Type: SEQUENCE; Schema: deapp; Owner: -
--
CREATE SEQUENCE de_variant_population_info_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: de_variant_population_info; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_variant_population_info (
    variant_population_info_id bigint DEFAULT nextval('de_variant_population_info_seq'::regclass) NOT NULL,
    dataset_id character varying(50),
    info_name character varying(100),
    description text,
    type character varying(30),
    number character varying(10)
);

--
-- Name: de_variant_population_info de_variant_population_info_id_idx; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_population_info
    ADD CONSTRAINT de_variant_population_info_id_idx PRIMARY KEY (variant_population_info_id);

--
-- Name: variant_population_info_dataset_name; Type: INDEX; Schema: deapp; Owner: -
--
CREATE INDEX variant_population_info_dataset_name ON de_variant_population_info USING btree (dataset_id, info_name);

--
-- Name: de_variant_population_info de_variant_population_info_fk; Type: FK CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_variant_population_info
    ADD CONSTRAINT de_variant_population_info_fk FOREIGN KEY (dataset_id) REFERENCES de_variant_dataset(dataset_id);

