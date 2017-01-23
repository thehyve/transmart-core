--
-- Name: bio_content; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_content (
    bio_file_content_id bigint NOT NULL,
    file_name character varying(1000),
    repository_id bigint,
    location character varying(400),
    title character varying(1000),
    abstract character varying(2000),
    file_type character varying(200) NOT NULL,
    etl_id bigint,
    etl_id_c character varying(30),
    study_name character varying(30),
    cel_location character varying(300),
    cel_file_suffix character varying(30)
);

--
-- Name: external_file_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_content
    ADD CONSTRAINT external_file_pk PRIMARY KEY (bio_file_content_id);

--
-- Name: bio_content_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_content_pk ON bio_content USING btree (bio_file_content_id);

--
-- Name: tf_trg_bio_file_content_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_file_content_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_FILE_CONTENT_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_FILE_CONTENT_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_bio_file_content_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_file_content_id BEFORE INSERT ON bio_content FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_file_content_id();

--
-- Name: ext_file_cnt_cnt_repo_fk; Type: FK CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_content
    ADD CONSTRAINT ext_file_cnt_cnt_repo_fk FOREIGN KEY (repository_id) REFERENCES bio_content_repository(bio_content_repo_id);

