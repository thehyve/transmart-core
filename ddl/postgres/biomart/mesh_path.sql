-- Table used by SORL query in the view VW_FACETED_SEARCH_DISEASE

CREATE TABLE biomart.mesh_path
(
  unique_id character varying(20) NOT NULL,
  mesh_name character varying(200),
  child_number character varying(200),
  parent_number character varying(200),
  id_path character varying(500),
  CONSTRAINT mesh_path_pkey PRIMARY KEY (unique_id)
  USING INDEX TABLESPACE indx
)
WITH (
  OIDS=FALSE
)
TABLESPACE biomart;
ALTER TABLE biomart.mesh_path OWNER TO biomart;
GRANT ALL ON TABLE biomart.mesh_path TO biomart;
GRANT ALL ON TABLE biomart.mesh_path TO tm_cz;
GRANT SELECT ON TABLE biomart.mesh_path TO biomart_user;
