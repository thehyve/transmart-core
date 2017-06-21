
main <- function() {

    if (!exists('parseInput')) {
        stop("_core/input.R is not properly sourced")
    }
    return(list(success=TRUE))
}
