--
-- Name: bio_lit_ref_data; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_lit_ref_data (
    bio_lit_ref_data_id bigint NOT NULL,
    etl_id character varying(50),
    component character varying(100),
    component_class character varying(250),
    gene_id character varying(50),
    molecule_type character varying(50),
    variant character varying(250),
    reference_type character varying(50),
    reference_id character varying(250),
    reference_title character varying(2000),
    back_references character varying(1000),
    study_type character varying(250),
    disease character varying(250),
    disease_icd10 character varying(250),
    disease_mesh character varying(250),
    disease_site character varying(250),
    disease_stage character varying(250),
    disease_grade character varying(250),
    disease_types character varying(250),
    disease_description character varying(1000),
    physiology character varying(250),
    stat_clinical character varying(500),
    stat_clinical_correlation character varying(250),
    stat_tests character varying(500),
    stat_coefficient character varying(500),
    stat_p_value character varying(100),
    stat_description character varying(1000)
);

--
-- Name: bio_lit_ref_data bio_lit_ref_data_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_lit_ref_data
    ADD CONSTRAINT bio_lit_ref_data_pk PRIMARY KEY (bio_lit_ref_data_id);

--
-- Name: tf_trg_bio_lit_ref_data_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_lit_ref_data_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.BIO_LIT_REF_DATA_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_LIT_REF_DATA_ID ;
       end if;
       RETURN NEW;
end;
$$;

--
-- Name: bio_lit_ref_data trg_bio_lit_ref_data_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_lit_ref_data_id BEFORE INSERT ON bio_lit_ref_data FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_lit_ref_data_id();

