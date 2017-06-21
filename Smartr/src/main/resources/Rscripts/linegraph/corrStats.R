main <- function(params) {
    df <- runResults$data_matrix
    # we only care about categoric data in this script
    cat.df <- df[df$type == "categoric", ]

    patientIDs <- unique(cat.df$patientID)
    bioMarkers <- unique(cat.df$bioMarker)
    timeIntegers <- unique(cat.df$timeInteger)

    # create binary vector that represents occurence of params$bioMarker at params$timePoint for every patient
    time.df <- cat.df[cat.df$timeInteger == params$timeInteger & cat.df$bioMarker == params$bioMarker, ]
    bin.vec_1 <- as.numeric(patientIDs %in% time.df$patientID)

    # do the same for every other timepoint and biomarker to compute correlations
    output <- data.frame(bioMarker=character(), timeInteger=integer(), corrCoef=numeric(), pValue=numeric())
    for (bioMarker in bioMarkers) {
        for (timeInteger in timeIntegers[timeIntegers != params$timeInteger]) {
            time.df <- cat.df[cat.df$timeInteger == timeInteger & cat.df$bioMarker == bioMarker, ]
            bin.vec_2 <- as.numeric(patientIDs %in% time.df$patientID)
            test <- cor.test(bin.vec_1, bin.vec_2, method="pearson")
            output <- rbind(output, data.frame(bioMarker=bioMarker,
                                               timeInteger=timeInteger,
                                               corrCoef=as.numeric(test$estimate),
                                               pValue=test$p.value))
        }
    }
    json <- toJSON(output, digits=I(17))
    return(json)
}
