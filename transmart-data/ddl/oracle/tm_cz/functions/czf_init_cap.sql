--
-- Type: FUNCTION; Owner: TM_CZ; Name: CZF_INIT_CAP
--
  CREATE OR REPLACE FUNCTION "TM_CZ"."CZF_INIT_CAP" (
     text_to_parse IN VARCHAR2
	-- text_delimiter IN VARCHAR2
)
--	text string returned with words initcapped except for any in category_path_excluded_words

RETURN VARCHAR2

AS
   start_pos 		NUMBER;
   end_pos   		NUMBER;
   string_length 	INTEGER;
   string_tokens 	VARCHAR2(32676);
   counter 			INTEGER;
   token_value 		VARCHAR2(1000);
   text_delimiter 	char(1);
   noInitCap 		boolean;

   --	create array to hold strings that will not be initcapped

	type excluded_aat is table of category_path_excluded_words%ROWTYPE index by PLS_INTEGER;
	excludedText excluded_aat;
	exclCt PLS_INTEGER;

   --	text to return
   initcap_text varchar2(1000);

BEGIN
  -------------------------------------------------------------------------------
   -- Performs custom initcap for category paths where specific text strings are
   -- excluded from the process.  Strings are delimited by a space.  The \ in
   -- the category path are converted to ' \ ' before parsing.

   -- JEA@20091001 - First rev.
   -- Copyright ¿ 2009 Recombinant Data Corp.
   -------------------------------------------------------------------------------

	--	Load exclusion text

	select excluded_text
		bulk collect into excludedText
		from category_path_excluded_words;

	--	Add a delimiter to the end of the string so we dont lose last value and
	--	surround \ with spaces

	text_delimiter := ' ';
	string_tokens := replace(text_to_parse,'\',' \ ') || text_delimiter;

	--get length of string
	string_length := length(string_tokens);

	--set start and end for first token
	start_pos := 1;
	end_pos   := instr(string_tokens,text_delimiter,1,1);
	counter := 1;

	LOOP
		--	Get substring
		token_value := substr(string_tokens, start_pos, end_pos - start_pos);

		--	check if token_value is in excludedText, if yes, set indicator

		noInitCap := false;
		exclCt := excludedText.FIRST;

		while (exclCt is not null and not noInitCap)
		loop
			if token_value = excludedText(exclCt).excluded_text then
				noInitCap := true;
			end if;
			exclCt := excludedText.NEXT (exclCt);
		end loop;

		if noInitCap then
			initcap_text := initcap_text || token_value || ' ';
		else
			initcap_text := initcap_text || initcap(token_value) || ' ';
		end if;

		--Check to see if we are done
		IF end_pos = string_length
		THEN
			initcap_text := replace(rtrim(initcap_text,' '),' \ ','\');
			EXIT;
		ELSE
			-- Increment Start Pos and End Pos
			start_pos := end_pos + 1;
			--	increment counter
			counter := counter + 1;
			end_pos := instr(string_tokens, text_delimiter,1, counter);

		END IF;
  END LOOP;

  return initcap_text;

END CZF_INIT_CAP;
/
 
