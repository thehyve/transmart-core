if (!exists("remoteScriptDir")) {  #  Needed for unit-tests
  remoteScriptDir <- "resources/public/RScripts/_core"
}

inputUtils <- paste(remoteScriptDir, "/_core/input.R", sep="")
source(inputUtils)
