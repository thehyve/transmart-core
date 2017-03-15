--
-- Name: de_snp_subject_sorted_def; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_subject_sorted_def (
    snp_subject_sorted_def_id bigint NOT NULL,
    trial_name character varying(255),
    patient_position integer,
    patient_num bigint,
    subject_id character varying(255),
    bio_assay_platform_id bigint
);

--
-- Name: de_snp_subject_sorted_def sys_c0020607; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_snp_subject_sorted_def
    ADD CONSTRAINT sys_c0020607 PRIMARY KEY (snp_subject_sorted_def_id);

--
-- Name: tf_trg_de_subject_sorted_def_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_trg_de_subject_sorted_def_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
if coalesce(NEW.SNP_SUBJECT_SORTED_DEF_ID::text, '') = '' then
select nextval('deapp.SEQ_DATA_ID') into NEW.SNP_SUBJECT_SORTED_DEF_ID ;
end if;
RETURN NEW;
end;
$$;

--
-- Name: de_snp_subject_sorted_def trg_de_subject_sorted_def_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_de_subject_sorted_def_id BEFORE INSERT ON de_snp_subject_sorted_def FOR EACH ROW EXECUTE PROCEDURE tf_trg_de_subject_sorted_def_id();

--
-- Name: tf_trg_snp_subject_sorted_def_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_trg_snp_subject_sorted_def_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
if coalesce(NEW.SNP_SUBJECT_SORTED_DEF_ID::text, '') = '' then
select nextval('deapp.SEQ_DATA_ID') into NEW.SNP_SUBJECT_SORTED_DEF_ID ;
end if;
RETURN NEW;
end;
$$;


SET default_with_oids = false;

--
-- Name: de_snp_subject_sorted_def trg_snp_subject_sorted_def_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_snp_subject_sorted_def_id BEFORE INSERT ON de_snp_subject_sorted_def FOR EACH ROW EXECUTE PROCEDURE tf_trg_snp_subject_sorted_def_id();

