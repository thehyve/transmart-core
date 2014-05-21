--
-- Name: dump_table_to_csv(character varying, character varying, character varying); Type: FUNCTION; Schema: deapp; Owner: -
--
CREATE FUNCTION dump_table_to_csv(p_tname character varying, p_dir character varying, p_filename character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

        l_output        utl_file.file_type;
        l_theCursor     integer default dbms_sql.open_cursor;
        l_columnValue   varchar(4000);
        l_status        integer;
        l_query         varchar(1000)
                       default 'select * from ' || p_tname;
       l_colCnt        integer := 0;
       l_separator     varchar(1);
       l_descTbl       dbms_sql.desc_tab;
   
BEGIN
       l_output := utl_file.fopen( p_dir, p_filename, 'w' );
       EXECUTE 'alter session set nls_date_format=''dd-mon-yyyy hh24:mi:ss''
';

       dbms_sql.parse(  l_theCursor,  l_query, dbms_sql.native );
       dbms_sql.describe_columns( l_theCursor, l_colCnt, l_descTbl );

      for i in 1 .. l_colCnt loop
           utl_file.put( l_output, l_separator || '"' || l_descTbl(i).col_name || '"'
);
           dbms_sql.define_column( l_theCursor, i, l_columnValue, 4000 );
          l_separator := ',';
       end loop;
       utl_file.new_line( l_output );

       l_status := dbms_sql.execute(l_theCursor);

       while ( dbms_sql.fetch_rows(l_theCursor) > 0 ) loop
           l_separator := '';
           for i in 1 .. l_colCnt loop
               dbms_sql.column_value( l_theCursor, i, l_columnValue );
               utl_file.put( l_output, l_separator || l_columnValue );
               l_separator := ',';
           end loop;
           utl_file.new_line( l_output );
       end loop;
       dbms_sql.close_cursor(l_theCursor);
       utl_file.fclose( l_output );

       EXECUTE 'alter session set nls_date_format=''dd-MON-yy'' ';
   exception
       when others then
           EXECUTE 'alter session set nls_date_format=''dd-MON-yy'' ';
           raise;
   end;
 
$$;

