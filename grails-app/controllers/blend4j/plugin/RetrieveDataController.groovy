package blend4j.plugin

class RetrieveDataController  {

    def springSecurityService
    def retrieveDataService

    def JobExportToGalaxy = {
        final  galaxyURL = grailsApplication.config.com.galaxy.blend4j.galaxyURL;
        final tempFolderDirectory  = grailsApplication.config.com.recomdata.plugins.tempFolderDirectory
        final int idOfTheUser = springSecurityService.getPrincipal().id;
        def statusOK = retrieveDataService.saveStatusOfExport(params.nameOfTheExportJob,params.nameOfTheLibrary);
        if(statusOK){
            retrieveDataService.uploadExportFolderToGalaxy(galaxyURL, tempFolderDirectory.toString(),idOfTheUser, params.nameOfTheExportJob, params.nameOfTheLibrary );
            retrieveDataService.updateStatusOfExport(params.nameOfTheExportJob,"Done");
        }else{
            retrieveDataService.updateStatusOfExport(params.nameOfTheExportJob,"Error");
        }
    }
}