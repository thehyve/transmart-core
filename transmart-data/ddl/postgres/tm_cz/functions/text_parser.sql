--
-- Name: text_parser(text); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION text_parser(text_to_parse text) RETURNS text[]
    LANGUAGE plpgsql
    AS $$
DECLARE

   start_pos bigint;
   end_pos   bigint;
   string_length integer;
   string_tokens varchar(32676);
   counter integer;
   token_value varchar(100);

   list_values text_table;


BEGIN
  -------------------------------------------------------------------------------
   -- Populates a temp_token table with parsed values for any comma separated list.
   -- Requires a type so that multiple records can exist for different uses.
   -- KCR@20090106 - First rev.
   -- Copyright c 2009 Recombinant Data Corp.
   -------------------------------------------------------------------------------

  --Add a delimiter to the end of the string so we dont lose last value
  string_tokens := text_to_parse || ',';

  --Initialize the collection
  list_values := _text() ;

  --get length of string
  string_length := length(string_tokens);

  --set start and end for first token
  start_pos := 1;
  end_pos   := instr(string_tokens,',',1,1);
  counter := 1;
  LOOP
    --Get substring
    token_value := substr(string_tokens, start_pos, end_pos - start_pos);

    --add values to collection
   list_values.EXTEND;
   list_values(list_Values.LAST):= token_value;

    --Check to see if we are done
    IF end_pos = string_length
    THEN
      EXIT;
    ELSE
      -- Increment Start Pos and End Pos
      start_pos := end_pos + 1;
      --increment counter
      counter := counter + 1;
      end_pos := instr(string_tokens, ',',1, counter);

    END IF;
  END LOOP;

  return list_values;

END text_parser;
 
$$;

