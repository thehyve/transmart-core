main <- function(params) {
    y.vec <- params$yVec

    output <- list()

    mean <- mean(y.vec)
    sd <- sd(y.vec)

    output$mean <- mean
    output$sd <- sd
    toJSON(output)
}
