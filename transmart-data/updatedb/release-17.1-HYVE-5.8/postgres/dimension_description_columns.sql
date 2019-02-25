-- Add dimension_type and sort_index columns.
ALTER TABLE i2b2metadata.dimension_description ADD COLUMN dimension_type character varying(50);
ALTER TABLE i2b2metadata.dimension_description ADD COLUMN sort_index integer;

--
-- add documentation
--
COMMENT ON TABLE i2b2metadata.dimension_description IS 'All supported dimensions and their properties.';

COMMENT ON COLUMN i2b2metadata.dimension_description.name IS 'The name of the dimension.';
COMMENT ON COLUMN i2b2metadata.dimension_description.dimension_type IS 'Indicates whether the dimension represents subjects or observation attributes. [SUBJECT, ATTRIBUTE]';
COMMENT ON COLUMN i2b2metadata.dimension_description.sort_index IS 'Specifies a relative order between dimensions.';
COMMENT ON COLUMN i2b2metadata.dimension_description.value_type IS 'T for string, N for numeric, B for raw text and D for date values. [T, N, B, D]';
COMMENT ON COLUMN i2b2metadata.dimension_description.modifier_code IS 'The modifier code if the dimension is a modifier dimension';
COMMENT ON COLUMN i2b2metadata.dimension_description.size_cd IS 'Indicates the typical size of the dimension. [SMALL, MEDIUM, LARGE]';
COMMENT ON COLUMN i2b2metadata.dimension_description.density IS 'Indicates the typical density of the dimension. [DENSE, SPARSE]';
COMMENT ON COLUMN i2b2metadata.dimension_description.packable IS 'Indicates if dimensions values can be packed when serialising. NOT_PACKABLE is a good default. [PACKABLE, NOT_PACKABLE]';
