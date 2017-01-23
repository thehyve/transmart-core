package blend4j.plugin

import grails.converters.JSON

class RetrieveDataController  {

    def springSecurityService
    def retrieveDataService

    def JobExportToGalaxy = {
        final  galaxyURL = grailsApplication.config.com.galaxy.blend4j.galaxyURL;
        final tempFolderDirectory  = grailsApplication.config.com.recomdata.plugins.tempFolderDirectory
        final String idOfTheUser = springSecurityService.getPrincipal().username;

        def statusOK = retrieveDataService.saveStatusOfExport(params.nameOfTheExportJob,params.nameOfTheLibrary);
        if(statusOK){
            retrieveDataService.uploadExportFolderToGalaxy(galaxyURL, tempFolderDirectory.toString(),idOfTheUser, params.nameOfTheExportJob, params.nameOfTheLibrary );
            retrieveDataService.updateStatusOfExport(params.nameOfTheExportJob,"Done");
        }else{
            retrieveDataService.updateStatusOfExport(params.nameOfTheExportJob,"Error");
        }

        render([statusOk: statusOK] as JSON)
    }

    /**
     * Method that will create the get the list of jobs to show in the galaxy jobs tab
     */
    def getjobs = {
        def username = springSecurityService.getPrincipal().username
        def result = retrieveDataService.getjobs(username, "DataExport")

        response.setContentType("text/json")
        response.outputStream << result?.toString()
    }


}