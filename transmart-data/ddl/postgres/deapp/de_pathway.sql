--
-- Name: de_pathway; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_pathway (
    name character varying(300),
    description character varying(510),
    id bigint NOT NULL,
    type character varying(100),
    source character varying(100),
    externalid character varying(100),
    pathway_uid character varying(200),
    user_id bigint
);

--
-- Name: de_pathway de_pathway_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_pathway
    ADD CONSTRAINT de_pathway_pkey PRIMARY KEY (id);

--
-- Name: tf_trg_de_pathway_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_trg_de_pathway_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
       if NEW.ID is null then
 select nextval('deapp.SEQ_DATA_ID') into NEW.ID ;
end if;
       RETURN NEW;
end;
$$;

--
-- Name: de_pathway trg_de_pathway_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_de_pathway_id BEFORE INSERT ON de_pathway FOR EACH ROW EXECUTE PROCEDURE tf_trg_de_pathway_id();

