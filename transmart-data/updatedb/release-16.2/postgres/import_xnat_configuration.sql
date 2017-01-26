CREATE TABLE searchapp.import_xnat_configuration (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    url character varying(255) NOT NULL,
    username character varying(255) NOT NULL,
    project character varying(255) NOT NULL,
    node character varying(255) NOT NULL
);

ALTER TABLE ONLY searchapp.import_xnat_configuration
    ADD CONSTRAINT import_xnat_config_pk PRIMARY KEY (id);

CREATE OR REPLACE FUNCTION searchapp.tf_trg_import_xnat_config_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.ID is null then
          select nextval('searchapp.SEQ_SEARCH_DATA_ID') into NEW.ID ;
    end if;
RETURN NEW;
end;
$$;

CREATE TRIGGER trg_import_xnat_config_id BEFORE INSERT ON searchapp.import_xnat_configuration FOR EACH ROW EXECUTE PROCEDURE searchapp.tf_trg_import_xnat_config_id();

GRANT DELETE, UPDATE, SELECT, INSERT ON TABLE searchapp.import_xnat_configuration TO biomart_user;
GRANT ALL ON TABLE searchapp.import_xnat_configuration TO searchapp;
GRANT ALL ON TABLE searchapp.import_xnat_configuration TO tm_cz;