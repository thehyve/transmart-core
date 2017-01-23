package fm

import org.springframework.web.multipart.commons.CommonsMultipartFile;

import fm.FmFolder
import grails.converters.JSON;

class UploadFilesController {
    
    def uploadFilesService

    def index = { }
    
    def displayUpload = {
        def parentFolder = FmFolder.get(params.folderId)
        log.info("displayUpload template uploadFiles parentFolder '${parentFolder}'")
        render(template: "uploadFiles", plugin: "folderManagement", model:[parentFolder:parentFolder]);
    }
    
    def upload = {
        def msg = "Loading failed"
        def files = request.getFiles('qqfile').each { file ->
            msg = uploadFilesService.upload(file, params.parentId)
            log.debug(file.toString()+": "+msg)
        }
        log.info("upload msg '${msg}'")
        def result
        if(msg == "File successfully loaded") result = [success: true, folderId: params.parentId,
                                                        folderParentId: params.folderParentId]
        else result = [success: false, error: msg]
        render text: result as JSON, contentType: 'text/html'
    }
}
