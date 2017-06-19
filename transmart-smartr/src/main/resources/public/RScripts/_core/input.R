# give me only the loaded_variables that match my sourceLabel
get.loaded_variables.by.source <- function(sourceLabel, loaded_variables) {
    SoNoSu.labels <- names(loaded_variables)
    SoNoSu.labels <- sort(SoNoSu.labels) # for determinism
    matches <- grepl(paste("^", sourceLabel, sep=""), SoNoSu.labels) # which SoNoSu labels begin with sourceLabel
    loaded_variables[SoNoSu.labels[matches]]
}


dropEmpty <- function(df) {
  df[df$value != "",]
}

parseSubsets <- function(subsetDfs) {
  if (length(subsetDfs) == 0) {
    return(list())
  }
  df <- subsetDfs[[1]]
  colnames(df) <- c('patientID', 'value')
  df <- dropEmpty(df)
  if (length(subsetDfs) == 1) {
    return(df)
  }
  for (i in 2:length(subsetDfs)) {
    subset <- subsetDfs[[i]]
    colnames(subset) <- c('patientID', 'value')
    subset <- dropEmpty(subset)
    df <- rbind(df, subset)
  }
  df
}

parseDataPoints <- function(datapointsDfs) {
  if (length(datapointsDfs) == 0) {
    stop("No datapoints in box1 - cannot generate boxplot")
  }
  df <- datapointsDfs[[1]]
  concept <- rep(colnames(df)[2], nrow(df))
  colnames(df) <- c('patientID', 'value')
  df$concept   <- concept
  if (length(datapointsDfs) == 1) {
    return(df)
  }
  for (i in 2:length(datapointsDfs)) {
    pointsDF <- datapointsDfs[[i]]
    concept            <- rep( colnames(pointsDF)[2], nrow(pointsDF) )
    colnames(pointsDF) <- c('patientID', 'value')
    pointsDF$concept   <- concept
    df <- rbind(df, pointsDF)
  }
  df
}

#  Parse input should be the entrypoint for every workflow. It will segregate data per source.
#  It returns a list of lists. With source name (e.g. box1) as keys and named dataframes as values.
parseInput <- function(input) {
#  validateLoadedVariables(input) // Fixme: Good idea but it is not working
  dataLabels <- names(input)
  splitted <- strsplit(dataLabels, "_")
  sources <- sapply(splitted, function(x) { x[[1]] })
  sources <- unique(sources)
  labelsOrganizedBySource <- list()
  for (dataSource in sources) {
    regexpPattern <- paste(dataSource,"_",sep="")
    indices <- grep(dataLabels, pattern =  regexpPattern)
    labelsOrganizedBySource[[dataSource]] <- input[indices]
  }
  labelsOrganizedBySource
}

#  This function should not be called directly - it is called during parseInput. It checks if the input is well formed.
validateLoadedVariables <- function(loadedVariablesList) {
    isAList <- is.list(loadedVariablesList)
    if (!isAList) {
        offendingType <- class(loadedVariablesList)
        stop(paste("loaded_variables must be a list. Is a: ",offendingType,sep=""))
    }
    for (df in loadedVariablesList) {
        if (!is.data.frame(df)) {
            offendingType <- class(df)
            stop(paste("loaded_variables must contain only data.frames. Found: ",offendingType,sep=""))
        }
    }
    labels <- names(loadedVariablesList)
    for (label in labels) {
        if (!isValidLabel(label) ){
            stop(paste("Invalid label in loaded_variables:",label))
        }
    }
    TRUE
}

# TRUE if the dataframe contains HighDimensional data
isHDD <- function(df) {
    atLeastThreeColumns <- ncol(df) > 2
    hasRowLabel   <- colnames(df)[1] == "Row.Label"
    hasBioMarker   <- colnames(df)[2] == "Bio.marker"
    measurements <- getHDDMeasurements(df)
    remainingColumnsNumeric <- all(apply(measurements, 2 ,FUN = is.numeric))
    return( atLeastThreeColumns & hasRowLabel & hasBioMarker & remainingColumnsNumeric )
}

# TRUE if the dataframe contains Clinical Data
isClinical <- function(df) {
   hasTwoColumns <- ncol(df) == 2
   hasRowLabel   <- colnames(df)[1] == "Row.Label"
   return(hasTwoColumns & hasRowLabel)
}

# TRUE if the dataframe is numeric clinical
isClinicalNumeric <- function(df) {
    return( isClinical(df) & is.numeric(df[[2]]) )
}

# TRUE if the dataframe is categorical clinical
isClinicalCategorical <- function() {
    return( isClinical(df) & !is.numeric(df[[2]]) )
}

# Extract measurements only from an HDD data frame
getHDDMeasurements <- function(df) {
  if (ncol(df) > 3){
    return(df[, 3:ncol(df)])
  }
  else {
    return( df[3] )
  }
}

# Checks if the label of a loaded_variable dataframe conforms to the required naming.
isValidLabel <- function(label) {
    expected_format_names <- "^[[:alpha:]]+[[:digit:]]+_n[[:digit:]]+_s[[:digit:]]+$"
    grepl(expected_format_names, label)
}

mergeHDD <- function(listOfHdd){
  df <- listOfHdd[[1]]

  #test if the different data.frames all contain the exact same set of probe IDs/metabolites/etc, independent of order.
  row.Labels<- df$Row.Label

  for(i in 1:length(listOfHdd)){
    if(!all(listOfHdd[[i]]$Row.Label %in% row.Labels) | !all(row.Labels %in% listOfHdd[[i]]$Row.Label) ){
      assign("errors", "Mismatched probe_ids - different platform used?", envir = .GlobalEnv)
    }
  }

  #merge data.frames
  expected.rowlen <- nrow(df)
  labels <- names(listOfHdd)
  df <- add.subset.label(df,labels[1])

  if(length(listOfHdd) > 1){
    for(i in 2:length(listOfHdd)){
      df2 <- listOfHdd[[i]]
      label <- labels[i]
      df2 <- add.subset.label(df2,label)
      df <- merge(df, df2 ,by = c("Row.Label","Bio.marker"), all = T)
      if(nrow(df) != expected.rowlen){
        assign("errors", "Mismatched probe_ids - different platform used?", envir = .GlobalEnv)
      }
    }
  }
  return(df)
}
