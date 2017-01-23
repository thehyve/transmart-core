DO $$
DECLARE
    table_name text;
    cur_ts     text;
    index_name text;
    command    text;
    spec       text[][];
    pair       text[];
BEGIN
    RAISE NOTICE 'Started assigning tablespaces';

    spec := ARRAY[
        ['tm_cz', 'transmart'],
        ['tm_lz', 'transmart'],
        ['tm_wz', 'transmart'],
        ['i2b2demodata', 'transmart'],
        ['i2b2metadata', 'transmart'],
        ['deapp', 'transmart'],
        ['searchapp', 'transmart'],
        ['biomart', 'transmart'],
        ['galaxy', 'transmart'],
        ['fmapp', 'transmart'],
        ['amapp', 'transmart'],
        ['ts_batch', 'transmart']
    ];
    FOREACH pair SLICE 1 IN ARRAY spec LOOP
        -- Assign tables' tablespaces
        FOR table_name, cur_ts IN
                SELECT tablename, tablespace FROM pg_tables WHERE schemaname = pair[1] LOOP
            IF cur_ts = pair[2] THEN
                CONTINUE;
            END IF;

            RAISE NOTICE 'Current tablespace for %.% is %; changing to %',
                    pair[1], table_name, cur_ts, pair[2];

            command = 'ALTER TABLE ' || pair[1] || '.' || quote_ident(table_name) ||
                    ' SET TABLESPACE ' || pair[2];
            EXECUTE(command);
        END LOOP;

        -- Assign indexes' tablespaces
        FOR index_name, cur_ts IN
                SELECT indexname, tablespace FROM pg_indexes WHERE schemaname = pair[1] LOOP
            IF cur_ts = 'indx' THEN
                CONTINUE;
            END IF;

            RAISE NOTICE 'Current tablespace for index % is %; changing to indx',
                    index_name, cur_ts;

            command = 'ALTER INDEX ' || pair[1] || '.' || quote_ident(index_name) ||
                    ' SET TABLESPACE indx';
            EXECUTE(command);
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Finished assigning tablespaces';

END;
$$ LANGUAGE plpgsql;

-- vim: ft=plsql ts=4 sw=4 et:
