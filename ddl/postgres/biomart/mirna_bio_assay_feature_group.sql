--
-- Name: mirna_bio_assay_feature_group; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE mirna_bio_assay_feature_group (
    bio_assay_feature_group_id bigint NOT NULL,
    feature_group_name character varying(100) NOT NULL,
    feature_group_type character varying(50) NOT NULL
);
--
-- Name: tf_trg_mirna_bio_assay_f_g_id; Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_mirna_bio_assay_f_g_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.BIO_ASSAY_FEATURE_GROUP_ID is null then
 select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_ASSAY_FEATURE_GROUP_ID ;
if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_mirna_bio_assay_f_g_id(); Type: TRIGGER; Schema: biomart; Owner: -
--
  CREATE TRIGGER trg_mirna_bio_assay_f_g_id BEFORE INSERT ON mirna_bio_assay_feature_group FOR EACH ROW EXECUTE PROCEDURE tf_trg_mirna_bio_assay_f_g_id();

--
-- Name: mirna_bio_asy_feature_grp_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY mirna_bio_assay_feature_group
    ADD CONSTRAINT mirna_bio_asy_feature_grp_pk PRIMARY KEY (bio_assay_feature_group_id);
