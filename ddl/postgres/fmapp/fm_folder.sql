--
-- Name: fm_folder; Type: TABLE; Schema: fmapp; Owner: -
--
CREATE TABLE fm_folder (
    folder_id bigint,
    folder_name character varying(1000) NOT NULL,
    folder_full_name character varying(1000) NOT NULL,
    folder_level bigint NOT NULL,
    folder_type character varying(100) NOT NULL,
    folder_tag character varying(50),
    active_ind character(1) NOT NULL,
    parent_id bigint,
    description character varying(2000),
    PRIMARY KEY (folder_id)
);
--
-- Name: tf_trg_fm_folder_id; Type: FUNCTION; Schema: fmapp; Owner: -
--
CREATE FUNCTION tf_trg_fm_folder_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.FOLDER_ID is null then
 select nextval('fmapp.SEQ_FM_ID') into NEW.FOLDER_ID ;
if;
       RETURN NEW;
end;
$$;

--
-- Name: trg_fm_folder_id(); Type: TRIGGER; Schema: fmapp; Owner: -
--
  CREATE TRIGGER trg_fm_folder_id BEFORE INSERT ON fm_folder FOR EACH ROW EXECUTE PROCEDURE tf_trg_fm_folder_id();
 
--
-- Type: TRIGGER; Owner: FMAPP; Name: TRG_FM_FOLDER_UID
--

-- insert in table if UID is not already there

CREATE FUNCTION tf_trg_fm_folder_uid() RETURNS trigger
LANGUAGE plpgsql
    AS $$
DECLARE
  rec_count bigint;
BEGIN
  SELECT COUNT(*) INTO rec_count 
  FROM fm_data_uid 
  WHERE fm_data_id = new.folder_id;
  
  if rec_count = 0 then
    insert into fmapp.fm_data_uid (fm_data_id, unique_id, fm_data_type)
    values (NEW.folder_id, fm_folder_uid(NEW.folder_id), 'FM_FOLDER');
  end if;
end;
$$;

-- ALTER TRIGGER fmapp.trg_fm_folder_uid;
CREATE TRIGGER trg_fm_folder_uid AFTER INSERT ON fm_folder FOR EACH ROW EXECUTE PROCEDURE tf_trg_fm_folder_uid();
