--
-- Name: annotation; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE annotation (
    platform character varying(50),
    probeset character varying(50),
    gene_descr character varying(4000),
    gene_symbol character varying(4000),
    gene_id character varying(50),
    organism character varying(200),
    id bigint NOT NULL
);

--
-- Name: annotation_pk; Type: CONSTRAINT; Schema: biomart; Owner: -
--
ALTER TABLE ONLY annotation
    ADD CONSTRAINT annotation_pk PRIMARY KEY (id);

