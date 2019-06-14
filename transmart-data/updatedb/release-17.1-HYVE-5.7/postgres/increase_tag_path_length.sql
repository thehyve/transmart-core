-- Increase the length of tag paths to 700.
alter table i2b2metadata.i2b2_tags alter column path type character varying(700);
