--
-- Name: bio_asy_analysis_pltfm; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_asy_analysis_pltfm (
    bio_asy_analysis_pltfm_id bigint NOT NULL,
    platform_name character varying(200),
    platform_version character varying(200),
    platform_description character varying(1000)
);

--
-- Name: bio_asy_analysis_pltfm bio_assay_analysis_platform_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_asy_analysis_pltfm
    ADD CONSTRAINT bio_assay_analysis_platform_pk PRIMARY KEY (bio_asy_analysis_pltfm_id);

--
-- Name: bio_asy_analysis_pltfm_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_asy_analysis_pltfm_pk ON bio_asy_analysis_pltfm USING btree (bio_asy_analysis_pltfm_id);

--
-- Name: tf_trg_bio_asy_analysis_pltfm_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_asy_analysis_pltfm_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_ASY_ANALYSIS_PLTFM_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_ASY_ANALYSIS_PLTFM_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: bio_asy_analysis_pltfm trg_bio_asy_analysis_pltfm_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_asy_analysis_pltfm_id BEFORE INSERT ON bio_asy_analysis_pltfm FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_asy_analysis_pltfm_id();

