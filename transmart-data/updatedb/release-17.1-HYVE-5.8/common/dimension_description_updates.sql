--
-- Add documentation
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

-- Fill in columns values for known dimensions

update i2b2metadata.dimension_description set sort_index = 1 where name = 'patient';
update i2b2metadata.dimension_description set dimension_type = 'SUBJECT', sort_index = 2 where name = 'Diagnosis ID';
update i2b2metadata.dimension_description set dimension_type = 'SUBJECT', sort_index = 3 where name = 'Biosource ID';
update i2b2metadata.dimension_description set dimension_type = 'SUBJECT', sort_index = 4 where name = 'Biomaterial ID';
