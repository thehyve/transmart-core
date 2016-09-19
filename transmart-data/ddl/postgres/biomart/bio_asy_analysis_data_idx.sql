--
-- Name: bio_asy_analysis_data_idx; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_asy_analysis_data_idx (
    bio_asy_analysis_data_idx_id bigint NOT NULL,
    ext_type character varying(255) NOT NULL,
    field_idx numeric(10,0) NOT NULL,
    field_name character varying(255) NOT NULL,
    display_idx numeric(10,0) NOT NULL,
    display_name character varying(255) NOT NULL
);

--
-- Name: tf_trg_bio_asy_adi_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_asy_adi_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin     
      if coalesce(NEW.bio_asy_analysis_data_idx_id::text, '') = '' then          
        select nextval('biomart.seq_bio_data_id') into NEW.bio_asy_analysis_data_idx_id;       
      end if;       
       RETURN NEW;
  end;
$$;

--
-- Name: trg_bio_asy_adi_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_asy_adi_id BEFORE INSERT ON bio_asy_analysis_data_idx FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_asy_adi_id();

--
-- Name: seq_bio_data_id; Type: SEQUENCE; Schema: biomart; Owner: -
--
CREATE SEQUENCE seq_bio_data_id
    START WITH 391713417
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

