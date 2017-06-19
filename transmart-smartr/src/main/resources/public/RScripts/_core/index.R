if (!exists("remoteScriptDir")) {  #  Needed for unit-tests
  remoteScriptDir <- "web-app/HeimScripts/_core"
}

inputUtils <- paste(remoteScriptDir, "/_core/input.R", sep="")
source(inputUtils)
