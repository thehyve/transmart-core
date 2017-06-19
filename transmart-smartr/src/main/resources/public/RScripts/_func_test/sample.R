main <- function(data_to_pass, data_to_write) {
    write(data_to_write, file='test_file')
    list(a=1,b='foobar',passed=data_to_pass)
}
