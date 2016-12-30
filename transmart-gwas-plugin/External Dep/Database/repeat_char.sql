CREATE OR REPLACE FUNCTION TM_CZ.repeat_char (i_value varchar2, i_count integer, i_char VARCHAR2)
   return varchar2
is

--	use this function if version of Oracle does not support REPEAT string function
	v_loop_ct	integer;
	v_value		varchar2(4000);
	
begin

	if i_count < 1 then
		return i_value;
	end if;
	
	v_loop_ct := 1;
	v_value := i_value;
	
	while v_loop_ct <= i_count
	loop
		v_value := v_value || i_char;
		v_loop_ct := v_loop_ct + 1;
	end loop;
	
	return v_value;

end repeat_char;
		