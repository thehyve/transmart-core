--
-- Name: bio_data_correlation; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_correlation (
    bio_data_id bigint NOT NULL,
    asso_bio_data_id bigint NOT NULL,
    bio_data_correl_descr_id bigint NOT NULL,
    bio_data_correl_id bigint NOT NULL
);

--
-- Name: bio_data_correlation_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_correlation
    ADD CONSTRAINT bio_data_correlation_pk PRIMARY KEY (bio_data_correl_id);

--
-- Name: bdc_index1; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bdc_index1 ON bio_data_correlation USING btree (asso_bio_data_id);

--
-- Name: tf_trg_bio_data_correl_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_data_correl_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_DATA_CORREL_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_DATA_CORREL_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_bio_data_correl_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_data_correl_id BEFORE INSERT ON bio_data_correlation FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_data_correl_id();

--
-- Name: bio_marker_link_bio_marker_rel; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_correlation
    ADD CONSTRAINT bio_marker_link_bio_marker_rel FOREIGN KEY (bio_data_correl_descr_id) REFERENCES bio_data_correl_descr(bio_data_correl_descr_id);

