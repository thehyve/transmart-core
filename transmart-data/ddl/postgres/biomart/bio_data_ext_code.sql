--
-- Name: bio_data_ext_code; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_ext_code (
    bio_data_id bigint NOT NULL,
    code character varying(500) NOT NULL,
    code_source character varying(200),
    code_type character varying(200),
    bio_data_type character varying(100),
    bio_data_ext_code_id bigint NOT NULL,
    etl_id character varying(50)
);

--
-- Name: bio_data_ext_code_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_ext_code
    ADD CONSTRAINT bio_data_ext_code_pk PRIMARY KEY (bio_data_ext_code_id);

--
-- Name: bio_d_e_c_did_ct_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_d_e_c_did_ct_idx ON bio_data_ext_code USING btree (bio_data_id, code_type);

--
-- Name: bio_data_e_c_c_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_data_e_c_c_idx ON bio_data_ext_code USING btree (code);

--
-- Name: bio_data_e_c_t_idx; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_data_e_c_t_idx ON bio_data_ext_code USING btree (code_type);

--
-- Name: tf_trg_bio_data_ext_code_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_data_ext_code_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_DATA_EXT_CODE_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_DATA_EXT_CODE_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_bio_data_ext_code_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_data_ext_code_id BEFORE INSERT ON bio_data_ext_code FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_data_ext_code_id();

