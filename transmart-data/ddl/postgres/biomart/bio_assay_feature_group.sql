--
-- Name: bio_assay_feature_group; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_feature_group (
    bio_assay_feature_group_id bigint NOT NULL,
    feature_group_name character varying(100) NOT NULL,
    feature_group_type character varying(50) NOT NULL
);

--
-- Name: bio_asy_feature_grp_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_feature_group
    ADD CONSTRAINT bio_asy_feature_grp_pk PRIMARY KEY (bio_assay_feature_group_id);

--
-- Name: bio_asy_feature_grp_name_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_asy_feature_grp_name_idx ON bio_assay_feature_group USING btree (feature_group_name, bio_assay_feature_group_id);

--
-- Name: tf_trg_bio_assay_f_g_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_assay_f_g_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_ASSAY_FEATURE_GROUP_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_ASSAY_FEATURE_GROUP_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_bio_assay_f_g_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_assay_f_g_id BEFORE INSERT ON bio_assay_feature_group FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_assay_f_g_id();

