package fm
import java.net.UnknownHostException
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.InputStream
import org.springframework.web.multipart.commons.CommonsMultipartFile

import com.mongodb.DB
import com.mongodb.Mongo
import com.mongodb.MongoClient
import com.mongodb.gridfs.GridFS
import com.mongodb.gridfs.GridFSInputFile

import fm.FmData
import fm.FmFile
import fm.FmFolder
import grails.util.Holders
import org.transmart.mongo.MongoUtils
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.content.InputStreamBody

import groovyx.net.http.*

class UploadFilesService {

    boolean transactional = true
    def config = Holders.config
    def filestoreDirectory = config.com.recomdata.FmFolderService.filestoreDirectory
    def fmFolderService

    def upload(CommonsMultipartFile fileToUpload, String parentId){
        def fmFile
        def fileBytes
        try{
            def fileName = fileToUpload.getOriginalFilename().toString()
            def fileType = fileName.split("\\.", -1)[fileName.split("\\.",-1).length-1]
            def fileSize = fileToUpload.getSize()
            //create fmfile
            FmFolder fmFolder
            try {
                fmFolder = FmFolder.get(parentId)
                if (fmFolder == null) {
                    log.error("Folder with id " + parentId + " does not exist.")
                    return "Folder with id " + parentId + " does not exist."
                }
            } catch (NumberFormatException ex) {
                log.error("Loading failed: "+e.toString())
                return "Loading failed"
            }

            // Check if folder already contains file with same name.
            fmFile = fmFolder.fmFiles.find { it.originalName == fileName }
            // If it does, then use existing file record and increment its version.
            // Otherwise, create a new file.
            if (fmFile != null) {
                fmFile.fileVersion++
                fmFile.fileSize = fileSize
                fmFile.linkUrl = ""
                log.info("File = " + fileName + " (" + fmFile.id + ") - Existing")
            } else {
                fmFile = new FmFile(
                    displayName: fileName,
                    originalName: fileName,
                    fileType: fileType,
                    fileSize: fileSize,
                    filestoreLocation: "",
                    filestoreName: "",
                    linkUrl: ""
                )
                if (!fmFile.save(flush:true)) {
                    fmFile.errors.each {
                        log.error("File saving failed: "+it)
                    }
                    return "Loading failed: fmfile saving"
                }
                fmFolder.addToFmFiles(fmFile)
                if (!fmFolder.save(flush:true)) {
                    fmFolder.errors.each {
                        log.error("Folder saving failed: "+it)
                    }
                    return "Loading failed: fmfolder saving"
                }
            }
            fmFile.filestoreLocation = parentId
            fmFile.filestoreName = fmFile.id + "-" + fmFile.fileVersion + "." + fmFile.fileType;
            if (!fmFile.save(flush:true)) {
                fmFile.errors.each {
                    log.error("File saving failed: "+it)
                }
                return "Loading failed: file saving"
             }
            log.info("File = " + fmFile.filestoreName + " (" + fileName + ") - Stored")

            def useMongo=Holders.config.transmartproject.mongoFiles.enableMongo
            if(!useMongo){
                log.info "Writing to filestore file '" + filestoreDirectory + File.separator + parentId + File.separator + fmFile.filestoreName + "'"
                File filestoreDir = new File(filestoreDirectory + File.separator + parentId);
                if (!filestoreDir.exists()) {
                    if (!filestoreDir.mkdirs()) {
                        log.error("unable to create filestoredir " + filestoreDir.getPath());
                        return "Loading failed: unable to create filestoredir";
                    }
                }
                OutputStream outputStream = new FileOutputStream(new File(filestoreDirectory + File.separator + parentId + File.separator + fmFile.filestoreName))
                log.info "Copying from fileToUpload "+fileToUpload
                log.info "Create outputStream ${outputStream}"
                if(outputStream != null) {
                    fileBytes = new byte[1024]
                    int nread
                    InputStream inputStream = fileToUpload.inputStream
                    while((nread = inputStream.read(fileBytes)) != -1) {
                        outputStream.write(fileBytes,0,nread)
                    }
                    outputStream.close();
                    fmFolderService.indexFile(fmFile)
                    log.info("File successfully loaded: "+fmFile.id)
                    return "File successfully loaded"
                }
                else {
                    log.error "Unable to write to filestoreDirectory "+filestoreDirectory
                    return "Unable to write to filestoreDirectory"
                }
            } else {
                    if(Holders.config.transmartproject.mongoFiles.useDriver){
                    MongoClient mongo = new MongoClient(Holders.config.transmartproject.mongoFiles.dbServer,
                                                        Holders.config.transmartproject.mongoFiles.dbPort)
                    DB db = mongo.getDB( Holders.config.transmartproject.mongoFiles.dbName)
                    GridFS gfs = new GridFS(db)
                    GridFSInputFile file=gfs.createFile(fileToUpload.inputStream, fmFile.filestoreName)
                    file.setContentType(fileToUpload.contentType)
                    file.save()
                    mongo.close()
                    fmFolderService.indexFile(fmFile)
                    log.info("File successfully loaded: "+fmFile.id)
                    return "File successfully loaded"
                }else{
                    def apiURL = Holders.config.transmartproject.mongoFiles.apiURL
                    def apiKey = Holders.config.transmartproject.mongoFiles.apiKey
                    def http = new HTTPBuilder( apiURL+"insert/"+fmFile.filestoreName )
                    http.request(Method.POST) {request ->
                        headers.'apikey' = MongoUtils.hash(apiKey)
                        requestContentType: "multipart/form-data"
                        MultipartEntity multiPartContent = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
                        multiPartContent.addPart(fmFile.filestoreName,
                                                 new InputStreamBody(fileToUpload.inputStream,
                                                                     fileToUpload.contentType,
                                                                     fileToUpload.originalFilename))

                        request.setEntity(multiPartContent)

                        response.success = { resp ->
                            if(resp.status < 400){
                                fmFolderService.indexFile(fmFile)
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
            }
        }catch(Exception e){
            log.error("transfer error: "+e.toString())
            if(fmFile != null) fmFile.delete()
        }
    }

}
