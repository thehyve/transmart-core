--
-- Name: search_auth_principal; Type: TABLE; Schema: searchapp; Owner: -
--
CREATE TABLE search_auth_principal (
    id bigint NOT NULL,
    principal_type character varying(255),
    date_created timestamp without time zone NOT NULL,
    description character varying(255),
    last_updated timestamp without time zone NOT NULL,
    name character varying(255),
    unique_id character varying(255),
    enabled boolean
);

--
-- Name: pk_search_principal; Type: CONSTRAINT; Schema: searchapp; Owner: -
--
ALTER TABLE ONLY search_auth_principal
    ADD CONSTRAINT pk_search_principal PRIMARY KEY (id);

--
-- Name: tf_trg_search_au_prcpl_id(); Type: FUNCTION; Schema: searchapp; Owner: -
--
CREATE FUNCTION tf_trg_search_au_prcpl_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin     
 if(coalesce(NEW.ID::text, '') = '' or NEW.ID = -2000) then       
 select nextval('searchapp.SEQ_SEARCH_DATA_ID') into NEW.ID ;      
 end if;     RETURN NEW;
end;
$$;

--
-- Name: trg_search_au_prcpl_id; Type: TRIGGER; Schema: searchapp; Owner: -
--
CREATE TRIGGER trg_search_au_prcpl_id BEFORE INSERT ON search_auth_principal FOR EACH ROW EXECUTE PROCEDURE tf_trg_search_au_prcpl_id();

