main <- function() {
  kZipFilename <- 'analysis_data.zip'
  kFiles <- c(
    "params.json",
    "heatmap_data.tsv",
    "heatmap_orig_values.tsv"
  )
  kDefaultZipLocation = "/usr/bin/zip"
  
  # main function
  function() {
    lapply(kFiles, function(file) {
      if (!file.exists(file)) {
        stop(paste("File ", file, " doesn't exist. Has the analysis been run?"))
      }
    })
    if (file.exists(kDefaultZipLocation)) {
      status <- zip(kZipFilename, kFiles, zip = kDefaultZipLocation)
    } else {
      # relying on default for zip parameter fails on my box: sh: 1: : Permission denied
      status <- zip(kZipFilename, kFiles)
    }
    
    if (status != 0) {
      stop(paste("Zipping failed with status ", as.character(status)))
    }
    list()
  }
}()
