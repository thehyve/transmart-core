-- Add dimension_type and sort_index columns.
ALTER TABLE i2b2metadata.dimension_description ADD COLUMN dimension_type character varying(50);
ALTER TABLE i2b2metadata.dimension_description ADD COLUMN sort_index integer;
