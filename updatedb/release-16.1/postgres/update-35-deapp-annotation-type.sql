--
-- metabolomics annotation data as double precision
--

set search_path = deapp, pg_catalog;

ALTER TABLE deapp.de_subject_metabolomics_data
    ALTER COLUMN raw_intensity TYPE double precision,
    ALTER COLUMN raw_intensity SET NOT NULL,
    ALTER COLUMN log_intensity TYPE double precision,
    ALTER COLUMN log_intensity SET NOT NULL,
    ALTER COLUMN zscore TYPE double precision;
