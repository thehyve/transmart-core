--
-- Name: bio_data_correl_descr; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_correl_descr (
    bio_data_correl_descr_id bigint NOT NULL,
    correlation character varying(510),
    description character varying(1000),
    type_name character varying(200),
    status character varying(200),
    source character varying(100),
    source_code character varying(200)
);

--
-- Name: bio_data_correl_descr bio_marker_relationship_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_correl_descr
    ADD CONSTRAINT bio_marker_relationship_pk PRIMARY KEY (bio_data_correl_descr_id);

--
-- Name: bio_data_correl_descr_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_data_correl_descr_pk ON bio_data_correl_descr USING btree (bio_data_correl_descr_id);

--
-- Name: tf_trg_bio_mkr_correl_descr_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_mkr_correl_descr_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_DATA_CORREL_DESCR_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_DATA_CORREL_DESCR_ID ;
    end if;
RETURN NEW;
end;

$$;

--
-- Name: bio_data_correl_descr trg_bio_mkr_correl_descr_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_mkr_correl_descr_id BEFORE INSERT ON bio_data_correl_descr FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_mkr_correl_descr_id();

