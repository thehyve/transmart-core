CREATE TABLE searchapp.xnat_subject (
    tsmart_subjectid character varying(100),
    xnat_subjectid character varying(100),
    xnat_project character varying(80),
    id bigint NOT NULL
);

ALTER TABLE ONLY searchapp.xnat_subject
    ADD CONSTRAINT xnat_subject_pk PRIMARY KEY (id);

CREATE OR REPLACE FUNCTION searchapp.tf_trg_xnat_subject_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    if NEW.ID is null then
          select nextval('searchapp.SEQ_SEARCH_DATA_ID') into NEW.ID ;
    end if;
RETURN NEW;
end;
$$;

CREATE TRIGGER trg_xnat_subject_id BEFORE INSERT ON searchapp.xnat_subject FOR EACH ROW EXECUTE PROCEDURE searchapp.tf_trg_xnat_subject_id();

GRANT DELETE, UPDATE, SELECT, INSERT ON TABLE searchapp.xnat_subject TO biomart_user;
GRANT ALL ON TABLE searchapp.xnat_subject TO searchapp;
GRANT ALL ON TABLE searchapp.xnat_subject TO tm_cz;