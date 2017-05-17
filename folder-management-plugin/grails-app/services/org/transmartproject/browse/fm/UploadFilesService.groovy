package org.transmartproject.browse.fm

import grails.transaction.Transactional
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import com.mongodb.DB
import com.mongodb.MongoClient
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile
import grails.util.Holders
import org.transmart.mongo.MongoUtils;
import groovyx.net.http.HTTPBuilder;
import groovyx.net.http.Method;

import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.content.InputStreamBody

@Transactional
class UploadFilesService {

    def fmFolderService

    def upload(CommonsMultipartFile fileToUpload, String parentId){
        def fmFile;
        try{
            byte[] fileBytes=null;
            def fileName = fileToUpload.getOriginalFilename().toString()
            def fileType = fileName.split("\\.", -1)[fileName.split("\\.",-1).length-1]
            def fileSize = fileToUpload.getSize()

            //create fmfile
            FmFolder fmFolder;
            try {
                fmFolder = FmFolder.get(parentId);
                if (fmFolder == null) {
                    log.error("Folder with id " + parentId + " does not exist.")
                    return "Folder with id " + parentId + " does not exist.";
                }
            } catch (NumberFormatException ex) {
                log.error("Loading failed: "+e.toString())
                return "Loading failed";
            }

            // Check if folder already contains file with same name.
            fmFile = fmFolder.fmFiles.find { it.originalName == fileName }
            // If it does, then use existing file record and increment its version.
            // Otherwise, create a new file.
            if (fmFile != null) {
                fmFile.fileVersion++;
                fmFile.fileSize = fileSize;
                fmFile.linkUrl = "";
                log.info("File = " + fileName + " (" + fmFile.id + ") - Existing");
            } else {
                fmFile = new FmFile(
                        displayName: fileName,
                        originalName: fileName,
                        fileType: fileType,
                        fileSize: fileSize,
                        filestoreLocation: "",
                        filestoreName: "",
                        linkUrl: ""
                );
                if (!fmFile.save(flush:true)) {
                    fmFile.errors.each {
                        log.error("File saving failed: "+it)
                    }
                    return "Loading failed";
                }
                fmFolder.addToFmFiles(fmFile);
                if (!fmFolder.save(flush:true)) {
                    fmFolder.errors.each {
                        log.error("Folder saving failed: "+it)
                    }
                    return "Loading failed";
                }
            }
            fmFile.filestoreName = fmFile.id + "-" + fmFile.fileVersion + "." + fmFile.fileType;
            if (!fmFile.save(flush:true)) {
                fmFile.errors.each {
                    log.error("File saving failed: "+it)
                }
                return "Loading failed";
            }

            if(Holders.config.fr.sanofi.mongoFiles.useDriver){
                MongoClient mongo = new MongoClient(Holders.config.fr.sanofi.mongoFiles.dbServer, Holders.config.fr.sanofi.mongoFiles.dbPort)
                DB db = mongo.getDB( Holders.config.fr.sanofi.mongoFiles.dbName)
                GridFS gfs = new GridFS(db)
                GridFSInputFile file=gfs.createFile(fileToUpload.inputStream, fmFile.filestoreName)
                file.setContentType(fileToUpload.contentType)
                file.save()
                mongo.close()
                fmFolderService.indexFile(fmFile);
                log.info("File successfully loaded: "+fmFile.id)
                return "File successfully loaded"
            }else{
                def apiURL = Holders.config.fr.sanofi.mongoFiles.apiURL
                def apiKey = Holders.config.fr.sanofi.mongoFiles.apiKey
                def http = new HTTPBuilder( apiURL+"insert/"+fmFile.filestoreName )
                http.request(Method.POST) {request ->
                    headers.'apikey' = MongoUtils.hash(apiKey)
                    requestContentType: "multipart/form-data"
                    MultipartEntity multiPartContent = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
                    multiPartContent.addPart(fmFile.filestoreName, new InputStreamBody(fileToUpload.inputStream, fileToUpload.contentType, fileToUpload.originalFilename))

                    request.setEntity(multiPartContent)

                    response.success = { resp ->
                        if(resp.status < 400){
                            fmFolderService.indexFile(fmFile);
                            log.info("File successfully loaded: "+fmFile.id)
                            return "File successfully loaded"
                        }
                    }

                    response.failure = { resp ->
                        log.error("Problem during connection to API: "+resp.status)
                        if(fmFile!=null) fmFile.delete()
                        if(resp.status ==404){
                            return "Problem during connection to API"
                        }
                        return "Loading failed"
                    }
                }
            }
        }catch(Exception e){
            log.error("transfer error: "+e.toString())
            if(fmFile != null) fmFile.delete()
        }
    }

}
