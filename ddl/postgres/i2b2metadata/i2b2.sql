--
-- Name: i2b2_record_id_seq; Type: SEQUENCE; Schema: i2b2metadata; Owner: -
--
CREATE SEQUENCE i2b2_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: i2b2; Type: TABLE; Schema: i2b2metadata; Owner: -
--
CREATE TABLE i2b2 (
    c_hlevel numeric(22,0) NOT NULL,
    c_fullname character varying(700) NOT NULL,
    c_name character varying(2000) NOT NULL,
    c_synonym_cd character(1) NOT NULL,
    c_visualattributes character(3) NOT NULL,
    c_totalnum numeric(22,0),
    c_basecode character varying(50),
    c_metadataxml text,
    c_facttablecolumn character varying(50) NOT NULL,
    c_tablename character varying(150) NOT NULL,
    c_columnname character varying(50) NOT NULL,
    c_columndatatype character varying(50) NOT NULL,
    c_operator character varying(10) NOT NULL,
    c_dimcode character varying(700) NOT NULL,
    c_comment text,
    c_tooltip character varying(900),
    m_applied_path character varying(700) NOT NULL,
    update_date timestamp without time zone NOT NULL,
    download_date timestamp without time zone,
    import_date timestamp without time zone,
    sourcesystem_cd character varying(50),
    valuetype_cd character varying(50),
    m_exclusion_cd character varying(25),
    c_path character varying(700),
    c_symbol character varying(50),
    i2b2_id bigint,
    record_id integer DEFAULT nextval('i2b2_record_id_seq'::regclass) NOT NULL
);

--
-- Name: i2b2_c_fullname_uq; Type: CONSTRAINT; Schema: i2b2metadata; Owner: -
--
ALTER TABLE ONLY i2b2
    ADD CONSTRAINT i2b2_c_fullname_uq UNIQUE (c_fullname);

--
-- Name: i2b2_c_comment_char_length_idx; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE INDEX i2b2_c_comment_char_length_idx ON i2b2 USING btree (c_comment, char_length((c_fullname)::text));

--
-- Name: i2b2meta_idx_record_id; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE INDEX i2b2meta_idx_record_id ON i2b2 USING btree (record_id);

--
-- Name: idx_i2b2_basecode; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE INDEX idx_i2b2_basecode ON i2b2 USING btree (c_basecode, record_id, c_visualattributes);

--
-- Name: idx_i2b2_fullname_basecode; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE INDEX idx_i2b2_fullname_basecode ON i2b2 USING btree (c_fullname, c_basecode);

--
-- Name: ix_i2b2_source_system_cd; Type: INDEX; Schema: i2b2metadata; Owner: -
--
CREATE INDEX ix_i2b2_source_system_cd ON i2b2 USING btree (sourcesystem_cd);

--
-- Name: tf_trg_i2b2_id(); Type: FUNCTION; Schema: i2b2metadata; Owner: -
--
CREATE FUNCTION tf_trg_i2b2_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.I2B2_ID is null then
 select nextval('i2b2metadata.I2B2_ID_SEQ') into NEW.I2B2_ID ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_i2b2_id; Type: TRIGGER; Schema: i2b2metadata; Owner: -
--
CREATE TRIGGER trg_i2b2_id BEFORE INSERT ON i2b2 FOR EACH ROW EXECUTE PROCEDURE tf_trg_i2b2_id();

--
-- Name: i2b2_id_seq; Type: SEQUENCE; Schema: i2b2metadata; Owner: -
--
CREATE SEQUENCE i2b2_id_seq
    START WITH 496244
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

