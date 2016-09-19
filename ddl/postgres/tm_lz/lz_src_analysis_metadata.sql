--
-- Name: lz_src_analysis_metadata; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lz_src_analysis_metadata (
    study_id character varying(50),
    data_type character varying(50),
    analysis_name character varying(500),
    description character varying(2048),
    phenotype_ids character varying(250),
    population character varying(500),
    tissue character varying(500),
    genome_version character varying(50),
    genotype_platform_ids character varying(500),
    expression_platform_ids character varying(500),
    statistical_test character varying(500),
    research_unit character varying(500),
    sample_size character varying(500),
    cell_type character varying(500),
    pvalue_cutoff character varying(50),
    etl_date timestamp without time zone,
    filename character varying(500),
    status character varying(50),
    process_date timestamp without time zone,
    etl_id numeric(38,0),
    analysis_name_archived character varying(500),
    model_name character varying(500),
    model_desc character varying(500),
    sensitive_flag numeric(18,0),
    sensitive_desc character varying(500)
);

--
-- Name: tf_trg_lz_src_analysis_meta_id(); Type: FUNCTION; Schema: tm_lz; Owner: -
--
CREATE FUNCTION tf_trg_lz_src_analysis_meta_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin     
      if NEW.ETL_ID is null then
        select nextval('tm_lz.seq_etl_id') into NEW.ETL_ID ;       
      end if;       
    RETURN NEW;
end;
$$;


SET default_with_oids = false;

--
-- Name: trg_lz_src_analysis_meta_id; Type: TRIGGER; Schema: tm_lz; Owner: -
--
CREATE TRIGGER trg_lz_src_analysis_meta_id BEFORE INSERT ON lz_src_analysis_metadata FOR EACH ROW EXECUTE PROCEDURE tf_trg_lz_src_analysis_meta_id();

--
-- Name: seq_etl_id; Type: SEQUENCE; Schema: tm_lz; Owner: -
--
CREATE SEQUENCE seq_etl_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

