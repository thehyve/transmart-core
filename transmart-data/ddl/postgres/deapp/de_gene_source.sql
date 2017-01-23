--
-- Name: de_gene_SOURCE; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_gene_source (
    gene_source_id integer NOT NULL,
    gene_source_name character varying(255) NOT NULL,
    version character varying(32),
    release_date timestamp without time zone,
    url character varying(255)
);


--
-- Name: de_gene_source_pkey; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_gene_source
    ADD CONSTRAINT de_gene_source_pkey PRIMARY KEY (gene_source_id);


--
-- Name: u_gene_source_name; Type: CONSTRAINT; Schema: deapp; Owner: -
--
ALTER TABLE ONLY de_gene_source
    ADD CONSTRAINT u_gene_source_name UNIQUE (gene_source_name);

--
-- Name: tf_trg_de_gene_source_id(); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION tf_trg_de_gene_source_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
      if NEW.GENE_SOURCE_ID is null then
         select nextval('deapp.SEQ_DATA_ID') into NEW.GENE_SOURCE_ID ;
      end if;
RETURN NEW;
end;
$$;

--
-- Name: trg_de_gene_source_id; Type: TRIGGER; Schema: deapp; Owner: -
--
CREATE TRIGGER trg_de_gene_source_id BEFORE INSERT ON de_gene_source FOR EACH ROW EXECUTE PROCEDURE tf_trg_de_gene_source_id();

