DO $$
<<locals>>
DECLARE
    cur_owner    text;
    wanted_owner text;
    command      text;
    spec         text[];
    schema_name  text;
    obj_name     text;
    obj_type     char;
    exceptions   text[][];
    dummy        record;
BEGIN
    RAISE NOTICE 'Started assigning owners';

    -- One must be owner to create child tables and indexes
    exceptions := ARRAY[
        -- schema, object, owner
        ['biomart', 'heat_map_results',            'tm_cz'],
        ['deapp',   'de_subject_microarray_data',  'tm_cz'],
        ['deapp',   'de_subject_microarray_med',   'tm_cz'],
        ['deapp',   'de_subject_microarray_logs',  'tm_cz'],
        ['deapp',   'de_subject_acgh_data',        'tm_cz'],
        ['deapp',   'de_subject_rnaseq_data',      'tm_cz'],
        ['deapp',   'de_subject_metabolomics_data','tm_cz'],
        ['deapp',   'de_subject_mirna_data',       'tm_cz'],
        ['deapp',   'de_subject_protein_data',     'tm_cz'],
        ['deapp',   'de_subject_proteomics_data',  'tm_cz'],
        ['deapp',   'de_subject_rbm_data',         'tm_cz'],
        ['deapp',   'de_subject_rna_data',         'tm_cz'],
        ['tm_wz',   'wt_subject_metabolomics_logs','tm_cz'],
        ['tm_wz',   'wt_subject_metabolomics_calcs','tm_cz'],
        ['tm_wz',   'wt_subject_microarray_logs',  'tm_cz'],
        ['tm_wz',   'wt_subject_microarray_calcs', 'tm_cz'],
        ['tm_wz',   'wt_subject_mirna_logs',       'tm_cz'],
        ['tm_wz',   'wt_subject_mirna_calcs',      'tm_cz'],
        ['tm_wz',   'wt_subject_proteomics_logs',  'tm_cz'],
        ['tm_wz',   'wt_subject_proteomics_calcs', 'tm_cz'],
        ['tm_wz',   'wt_subject_rbm_logs',         'tm_cz'],
        ['tm_wz',   'wt_subject_rbm_calcs',        'tm_cz'],
        ['tm_wz',   'wt_subject_rna_logs',         'tm_cz'],
        ['tm_wz',   'wt_subject_rna_calcs',        'tm_cz'],
        ['tm_wz',   'wt_subject_rnaseq_logs',      'tm_cz'],
        ['tm_wz',   'wt_subject_rnaseq_calcs',     'tm_cz']
    ];

    -- Convert array to table
    CREATE TEMPORARY TABLE ownership_exceptions(
        schema_name,
        object_name,
        proper_owner)
        AS SELECT exceptions[idx][1], exceptions[idx][2], exceptions[idx][3]
        FROM generate_series(1, array_upper(exceptions, 1)) as b(idx);

    spec := ARRAY[
        'tm_cz',
        'tm_lz',
        'tm_wz',
        'i2b2demodata',
        'i2b2metadata',
        'amapp',
        'deapp',
        'fmapp',
        'biomart_user',
        'galaxy',
        'searchapp',
        'biomart',
        'ts_batch'
    ];

    FOREACH schema_name IN ARRAY spec LOOP

        FOR obj_name, obj_type, cur_owner IN
                SELECT name, kind, owner
                FROM public.schemas_tables_funcs
                WHERE nspname = schema_name AND NOT change_owner_skip
                LOOP

            SELECT proper_owner
            INTO wanted_owner
            FROM ownership_exceptions E
            WHERE E.schema_name = locals.schema_name
                AND E.object_name = locals.obj_name;

            IF NOT FOUND THEN
                wanted_owner := schema_name;
            END IF;

            IF cur_owner = wanted_owner THEN
                CONTINUE;
            END IF;

            RAISE NOTICE 'The owner of % %.% is %; changing to %',
                    CASE obj_type
                        WHEN 'T' THEN 'type'
                        WHEN 'r' THEN 'table'
                        WHEN 'S' THEN 'sequence'
                        WHEN 'v' THEN 'view'
                        WHEN 'f' THEN 'function'
                        WHEN 'a' THEN 'aggregate'
                        WHEN 's' THEN 'schema'
                    END,
                    schema_name,
                    obj_name,
                    cur_owner,
                    wanted_owner;

            -- ALTER TABLE can be used for all the types here
            command := 'ALTER ' ||
                    CASE obj_type
                        WHEN 'T' THEN 'TYPE'
                        WHEN 'f' THEN 'FUNCTION'
                        WHEN 'a' THEN 'AGGREGATE'
                        WHEN 's' THEN 'SCHEMA'
                        ELSE 'TABLE'
                    END || ' ' ||
                    CASE obj_type
                        WHEN 's' THEN '' -- do not qualify schema names
                        ELSE schema_name || '.'
                    END
                    || obj_name || ' OWNER TO ' || wanted_owner;

            EXECUTE(command);

        END LOOP;
    END LOOP;

    RAISE NOTICE 'Finished assigning owners';

END;
$$ LANGUAGE plpgsql;

-- vim: ft=plsql ts=4 sw=4 et:
