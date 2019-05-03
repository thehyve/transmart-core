-- Add dimension_type and sort_index columns.
ALTER TABLE i2b2metadata.dimension_description ADD COLUMN "DIMENSION_TYPE" VARCHAR2(50 BYTE);
ALTER TABLE i2b2metadata.dimension_description ADD COLUMN "SORT_INDEX" NUMBER;
