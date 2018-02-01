--
-- Name: bio_data_attribute; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_data_attribute (
    bio_data_attribute_id bigint NOT NULL,
    property_code character varying(200) NOT NULL,
    property_value character varying(200),
    bio_data_id bigint NOT NULL,
    property_unit character varying(100)
);

--
-- Name: bio_data_attribute bio_data_attr_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_data_attribute
    ADD CONSTRAINT bio_data_attr_pk PRIMARY KEY (bio_data_attribute_id);

--
-- Name: bio_data_attribute_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_data_attribute_pk ON bio_data_attribute USING btree (bio_data_attribute_id);

--
-- Name: tf_trg_bio_data_attr_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_data_attr_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_DATA_ATTRIBUTE_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_DATA_ATTRIBUTE_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: bio_data_attribute trg_bio_data_attr_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_data_attr_id BEFORE INSERT ON bio_data_attribute FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_data_attr_id();

