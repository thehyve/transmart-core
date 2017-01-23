CREATE OR REPLACE PROCEDURE TM_CZ.DELETE_RECURSIVE(
  arg_owner IN VARCHAR2, arg_table_name IN VARCHAR2
) AUTHID CURRENT_USER
AS
  CURSOR c1 IS WITH T(owner, table_name, leevel) AS (
    SELECT UPPER(arg_owner), UPPER(arg_table_name), 0 FROM DUAL

    UNION ALL

    SELECT C1.owner, C1.table_name, T.leevel + 1
    FROM all_constraints C1
      INNER JOIN all_constraints C2 ON (C1.R_CONSTRAINT_NAME = C2.CONSTRAINT_NAME)
      INNER JOIN T ON (C2.owner = T.owner AND C2.table_name = T.table_name)
    WHERE C1.CONSTRAINT_TYPE = 'R'
  )
               SELECT T.owner, T.table_name
               FROM T
               GROUP BY T.owner, T.table_name
               ORDER BY max(leevel) DESC;
BEGIN
  FOR R IN c1 LOOP
    EXECUTE IMMEDIATE 'DELETE FROM "' ||  R.owner || '"."' || R.table_name || '"';
  END LOOP;
END;
