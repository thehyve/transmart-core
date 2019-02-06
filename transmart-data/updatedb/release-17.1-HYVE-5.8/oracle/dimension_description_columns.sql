-- Add dimension_type and sort_index columns.
ALTER TABLE i2b2metadata.dimension_description ADD COLUMN "DIMENSION_TYPE" VARCHAR2(50 BYTE);
ALTER TABLE i2b2metadata.dimension_description ADD COLUMN "SORT_INDEX" NUMBER;

--
-- add documentation
--
COMMENT ON TABLE i2b2metadata.dimension_description IS 'All supported dimensions and their properties.';

COMMENT ON COLUMN dimension_description.name IS 'The name of the dimension.';
COMMENT ON COLUMN dimension_description.dimension_type IS 'Indicates whether the dimension represents subjects or observation attributes. [SUBJECT, ATTRIBUTE]';
COMMENT ON COLUMN dimension_description.sort_index IS 'Specifies a relative order between dimensions.';
COMMENT ON COLUMN dimension_description.value_type IS 'T for string, N for numeric, B for raw text and D for date values. [T, N, B, D]';
COMMENT ON COLUMN dimension_description.modifier_code IS 'The modifier code if the dimension is a modifier dimension';
COMMENT ON COLUMN dimension_description.size_cd IS 'Indicates the typical size of the dimension. [SMALL, MEDIUM, LARGE]';
COMMENT ON COLUMN dimension_description.density IS 'Indicates the typical density of the dimension. [DENSE, SPARSE]';
COMMENT ON COLUMN dimension_description.packable IS 'Indicates if dimensions values can be packed when serialising. NOT_PACKABLE is a good default. [PACKABLE, NOT_PACKABLE]';
