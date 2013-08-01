--
-- Name: bio_assay_analysis; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_analysis (
    analysis_name character varying(500),
    short_description character varying(510),
    analysis_create_date timestamp without time zone,
    analyst_id character varying(510),
    bio_assay_analysis_id bigint NOT NULL,
    analysis_version character varying(200),
    fold_change_cutoff double precision,
    pvalue_cutoff double precision,
    rvalue_cutoff double precision,
    bio_asy_analysis_pltfm_id bigint,
    bio_source_import_id bigint,
    analysis_type character varying(200),
    analyst_name character varying(250),
    analysis_method_cd character varying(50),
    bio_assay_data_type character varying(50),
    etl_id character varying(100),
    long_description character varying(4000),
    qa_criteria character varying(4000),
    data_count bigint,
    tea_data_count bigint,
    analysis_update_date date,
    lsmean_cutoff double precision
);

--
-- Name: bio_data_anl_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis
    ADD CONSTRAINT bio_data_anl_pk PRIMARY KEY (bio_assay_analysis_id);

--
-- Name: bio_assay_analysis_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_assay_analysis_pk ON bio_assay_analysis USING btree (bio_assay_analysis_id);

--
-- Name: tf_trg_bio_assay_analysis_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_assay_analysis_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin     if NEW.BIO_ASSAY_ANALYSIS_ID is null then          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_ASSAY_ANALYSIS_ID ;       end if;  RETURN NEW;  end;
$$;

--
-- Name: trg_bio_assay_analysis_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_assay_analysis_id BEFORE INSERT ON bio_assay_analysis FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_assay_analysis_id();

--
-- Name: bio_assay_ans_pltfm_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_analysis
    ADD CONSTRAINT bio_assay_ans_pltfm_fk FOREIGN KEY (bio_asy_analysis_pltfm_id) REFERENCES bio_asy_analysis_pltfm(bio_asy_analysis_pltfm_id);

--
-- Name: seq_bio_data_id; Type: SEQUENCE; Schema: biomart; Owner: -
--
CREATE SEQUENCE seq_bio_data_id
    START WITH 1082041
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

