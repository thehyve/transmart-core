--
-- Name: scale_bitset(bigint); Type: FUNCTION; Schema: biomart_user; Owner: -
--
CREATE OR REPLACE FUNCTION biomart_user.scale_bitset(min_digits bigint) RETURNS bit varying
    LANGUAGE plpgsql IMMUTABLE STRICT
    AS $_$
declare
        result_buffer bit varying;
        digits_in_buffer bigint;
begin
        digits_in_buffer := 1;
        result_buffer := B'0';
        WHILE digits_in_buffer < min_digits LOOP
        	result_buffer := bitcat(result_buffer, result_buffer);
        	digits_in_buffer := digits_in_buffer * 2;
        END LOOP;
        return result_buffer;
end;
$_$;

DO $$
BEGIN
  ASSERT length(biomart_user.scale_bitset(1)) = 1, 'scale_bitset(1) has to return 1.';
  ASSERT length(biomart_user.scale_bitset(100)) = 128, 'scale_bitset(100) has to return 128.';
  ASSERT length(biomart_user.scale_bitset(1073741824)) = 1073741824, 'scale_bitset(1073741824) has to return 1073741824 (maximum it could deal with, 2^30).';
  RAISE NOTICE 'scale_bitset tests have passed successfully.';
END$$;