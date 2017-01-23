--
-- Name: fm_folder; Type: TABLE; Schema: fmapp; Owner: -
--
CREATE TABLE fm_folder (
    folder_id bigint NOT NULL,
    folder_name character varying(1000) NOT NULL,
    folder_full_name character varying(1000) NOT NULL,
    folder_level bigint NOT NULL,
    folder_type character varying(100) NOT NULL,
    folder_tag character varying(50),
    active_ind boolean NOT NULL,
    parent_id bigint,
    description character varying(2000)
);

--
-- Name: fm_folder_pkey; Type: CONSTRAINT; Schema: fmapp; Owner: -
--
ALTER TABLE ONLY fm_folder
    ADD CONSTRAINT fm_folder_pkey PRIMARY KEY (folder_id);

--
-- Name: tf_trg_fm_folder_id(); Type: FUNCTION; Schema: fmapp; Owner: -
--
CREATE FUNCTION tf_trg_fm_folder_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
	begin
      if coalesce(NEW.FOLDER_ID::text, '') = '' then
        select nextval('FMAPP.SEQ_FM_ID') into NEW.FOLDER_ID ;
      end if;
	  if coalesce(NEW.FOLDER_FULL_NAME::text, '') = '' then
		if coalesce(NEW.PARENT_ID::text, '') = '' then
			select '\' || fm_folder_uid(NEW.folder_id) || '\' into NEW.FOLDER_FULL_NAME ;
		else
			select folder_full_name || fm_folder_uid(NEW.folder_id) || '\' into NEW.FOLDER_FULL_NAME 
      from fmapp.fm_folder
      where folder_id = NEW.parent_id;
		end if;
      end if;
      RETURN NEW;
  end;
$$;

--
-- Name: trg_fm_folder_id; Type: TRIGGER; Schema: fmapp; Owner: -
--
CREATE TRIGGER trg_fm_folder_id BEFORE INSERT ON fm_folder FOR EACH ROW EXECUTE PROCEDURE tf_trg_fm_folder_id();

--
-- Name: tf_trg_fm_folder_uid(); Type: FUNCTION; Schema: fmapp; Owner: -
--
CREATE FUNCTION tf_trg_fm_folder_uid() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
  rec_count bigint;
BEGIN
  SELECT COUNT(*) INTO rec_count 
  FROM fmapp.fm_data_uid 
  WHERE fm_data_id = new.folder_id;
  
  if rec_count = 0 then
    insert into fmapp.fm_data_uid (fm_data_id, unique_id, fm_data_type)
    values (NEW.folder_id, fm_folder_uid(NEW.folder_id), 'FM_FOLDER');
  end if;
RETURN NEW;
end;
$$;


SET default_with_oids = false;

--
-- Name: trg_fm_folder_uid; Type: TRIGGER; Schema: fmapp; Owner: -
--
CREATE TRIGGER trg_fm_folder_uid BEFORE INSERT ON fm_folder FOR EACH ROW EXECUTE PROCEDURE tf_trg_fm_folder_uid();

