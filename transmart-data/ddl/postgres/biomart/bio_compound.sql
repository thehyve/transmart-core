--
-- Name: bio_compound; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_compound (
    bio_compound_id bigint NOT NULL,
    cnto_number character varying(200),
    jnj_number character varying(200),
    cas_registry character varying(400),
    code_name character varying(300),
    generic_name character varying(200),
    brand_name character varying(200),
    chemical_name character varying(1000),
    mechanism character varying(400),
    product_category character varying(200),
    description character varying(1000),
    etl_id_retired bigint,
    etl_id character varying(50),
    source_cd character varying(100)
);

--
-- Name: bio_compound compounddim_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_compound
    ADD CONSTRAINT compounddim_pk PRIMARY KEY (bio_compound_id);

--
-- Name: bio_compound_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_compound_pk ON bio_compound USING btree (bio_compound_id);

--
-- Name: tf_trg_bio_compound_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_compound_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_COMPOUND_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_COMPOUND_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: bio_compound trg_bio_compound_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_compound_id BEFORE INSERT ON bio_compound FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_compound_id();

