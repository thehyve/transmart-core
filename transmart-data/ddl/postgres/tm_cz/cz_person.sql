--
-- Name: cz_person; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE cz_person (
    person_id bigint NOT NULL,
    f_name character varying(200),
    l_name character varying(200),
    m_name character varying(200),
    user_name character varying(20),
    role_code character varying(20),
    email_address character varying(100),
    mail_address character varying(200),
    cell_phone character varying(20),
    work_phone character varying(20)
);

--
-- Name: cz_person_pk; Type: CONSTRAINT; Schema: tm_cz; Owner: -
--
ALTER TABLE ONLY cz_person
    ADD CONSTRAINT cz_person_pk PRIMARY KEY (person_id);

--
-- Name: tf_trg_cz_personid(); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION tf_trg_cz_personid() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin            
if NEW.PERSON_ID is null then          
select nextval('tm_cz.SEQ_CZ_PERSON_ID') into NEW.PERSON_ID ;       end if;      RETURN NEW; end;
$$;

--
-- Name: trg_cz_personid; Type: TRIGGER; Schema: tm_cz; Owner: -
--
CREATE TRIGGER trg_cz_personid BEFORE INSERT ON cz_person FOR EACH ROW EXECUTE PROCEDURE tf_trg_cz_personid();

--
-- Name: seq_cz_person_id; Type: SEQUENCE; Schema: tm_cz; Owner: -
--
CREATE SEQUENCE seq_cz_person_id
    START WITH 41
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 2;

