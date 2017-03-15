--
-- Name: bio_content_repository; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_content_repository (
    bio_content_repo_id bigint NOT NULL,
    location character varying(510),
    active_y_n character(1),
    repository_type character varying(200) NOT NULL,
    location_type character varying(200)
);

--
-- Name: bio_content_repository external_file_repository_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY bio_content_repository
    ADD CONSTRAINT external_file_repository_pk PRIMARY KEY (bio_content_repo_id);

--
-- Name: bio_content_repository_pk; Type: INDEX; Schema: biomart; Owner: -
--
CREATE UNIQUE INDEX bio_content_repository_pk ON bio_content_repository USING btree (bio_content_repo_id);

--
-- Name: tf_trg_bio_content_repo_id(); Type: FUNCTION; Schema: biomart; Owner: -
--
CREATE FUNCTION tf_trg_bio_content_repo_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.BIO_CONTENT_REPO_ID is null then
          select nextval('biomart.SEQ_BIO_DATA_ID') into NEW.BIO_CONTENT_REPO_ID ;
    end if;
RETURN NEW;
end;
$$;

--
-- Name: bio_content_repository trg_bio_content_repo_id; Type: TRIGGER; Schema: biomart; Owner: -
--
CREATE TRIGGER trg_bio_content_repo_id BEFORE INSERT ON bio_content_repository FOR EACH ROW EXECUTE PROCEDURE tf_trg_bio_content_repo_id();

