--
-- Name: bio_assay_platform; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_assay_platform (
    bio_assay_platform_id bigint NOT NULL,
    platform_name character varying(200),
    platform_version character varying(200),
    platform_description character varying(2000),
    platform_array character varying(50),
    platform_accession character varying(20),
    platform_organism character varying(200),
    platform_vendor character varying(200),
    platform_type character varying(200),
    platform_technology character varying(200)
);

--
-- Name: bio_assay_platform_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_assay_platform
    ADD CONSTRAINT bio_assay_platform_pk PRIMARY KEY (bio_assay_platform_id);

--
-- Name: tf_trg_bio_assay_platform_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_assay_platform_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_ASSAY_PLATFORM_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_ASSAY_PLATFORM_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_bio_assay_platform_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_assay_platform_id BEFORE INSERT ON bio_assay_platform FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_assay_platform_id();

