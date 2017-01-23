--
-- Name: bio_concept_code; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_concept_code (
    bio_concept_code character varying(200),
    code_name character varying(200),
    code_description character varying(1000),
    code_type_name character varying(200),
    bio_concept_code_id bigint NOT NULL,
    filter_flag character(1) DEFAULT 0
);

--
-- Name: bio_concept_code_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_concept_code
    ADD CONSTRAINT bio_concept_code_pk PRIMARY KEY (bio_concept_code_id);

--
-- Name: bio_concept_code_uk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_concept_code
    ADD CONSTRAINT bio_concept_code_uk UNIQUE (bio_concept_code, code_type_name);

--
-- Name: bio_concept_code_type_index; Type: INDEX; Schema: biomart; Owner: -
--
CREATE INDEX bio_concept_code_type_index ON bio_concept_code USING btree (code_type_name);

--
-- Name: tf_trg_bio_concept_code_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_concept_code_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_CONCEPT_CODE_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_CONCEPT_CODE_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_bio_concept_code_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_concept_code_id BEFORE INSERT ON bio_concept_code FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_concept_code_id();

