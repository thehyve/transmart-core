-- Increase the length of tag paths to 700.
alter table I2B2METADATA.I2B2_TAGS modify ("PATH" VARCHAR2(700 BYTE));
