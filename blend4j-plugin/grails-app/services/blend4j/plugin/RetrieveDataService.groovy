package blend4j.plugin

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory
import com.github.jmchilton.blend4j.galaxy.LibrariesClient
import com.github.jmchilton.blend4j.galaxy.beans.FileLibraryUpload
import com.github.jmchilton.blend4j.galaxy.beans.Library
import com.github.jmchilton.blend4j.galaxy.beans.LibraryContent
import com.github.jmchilton.blend4j.galaxy.beans.LibraryFolder
import com.recomdata.transmart.domain.i2b2.AsyncJob
import com.sun.jersey.api.client.ClientResponse
import grails.transaction.Transactional
import org.apache.commons.lang.StringUtils
import org.json.JSONArray
import org.json.JSONObject

@Transactional
class RetrieveDataService {

    def saveStatusOfExport(String nameOfTheExportJob, String nameOfTheLibrary) {
        try{
            def newJob = new StatusOfExport();
            newJob.jobName = nameOfTheExportJob;
            newJob.jobStatus = "Started";
            newJob.lastExportName = nameOfTheLibrary;
            newJob.lastExportTime = new Date();
            newJob.save();
        }catch(e){
            log.error("The export job for galaxy couldn't be saved")
            return false;
        }
        return true;
    }

    def updateStatusOfExport(String nameOfTheExportJob, String newState) {
        try{
            def idOfTheExportJob = getLatest(StatusOfExport.findAllByJobName(nameOfTheExportJob)).id;
            def newJob = StatusOfExport.get(idOfTheExportJob);
            newJob.jobStatus = newState;
            newJob.save();
        }catch(e){
            log.error("The export job for galaxy couldn't be updated")
            return false;
        }
        return true;
    }

    def uploadExportFolderToGalaxy(String galaxyURL, String  tempFolderDirectory, String idOfTheUser,  String nameOfTheExportJob, String nameOfTheLibrary){

        def apiKey = GalaxyUserDetails.findByUsername(idOfTheUser).getGalaxyKey();
        final String email = GalaxyUserDetails.findByUsername(idOfTheUser).getMailAddress();
        final GalaxyInstance galaxyInstance = GalaxyInstanceFactory.get(galaxyURL, apiKey);
        String tempDir = tempFolderDirectory.toString() + "/" + nameOfTheExportJob;

        final Library library = new Library(nameOfTheLibrary+ " - " + email);
        final LibrariesClient client = galaxyInstance.getLibrariesClient();
        final Library persistedLibrary = client.createLibrary(library);
        final LibraryContent rootFolder = client.getRootFolder(persistedLibrary.getId());
        final LibraryFolder folder = new LibraryFolder();
        folder.setName(nameOfTheExportJob)
        folder.setFolderId(rootFolder.getId());

        LibraryFolder resultFolder = client.createFolder(persistedLibrary.getId(), folder);
        assert resultFolder.getName().equals(nameOfTheExportJob);
        assert resultFolder.getId() != null;

        File repoFolder = new File(tempDir);
        File[] listOfFiles = repoFolder.listFiles();
        createFoldersAndFiles(listOfFiles, resultFolder, client, persistedLibrary.getId());
    }

    def createFoldersAndFiles = { File[] listOfFiles, LibraryFolder rootFolder, LibrariesClient client, String persistedLibraryId ->
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                try {
                    File testFile = listOfFiles[i];
                    FileLibraryUpload upload = new FileLibraryUpload();
                    upload.setFolderId(rootFolder.getId());
                    upload.setName(testFile.getName().toString());
                    upload.setFileType("tabular");
                    upload.setFile(testFile);
                    ClientResponse resultFile = client.uploadFile(persistedLibraryId, upload);
                    assert resultFile.getStatus() == 200: resultFile.getEntity(String.class);
                } catch (final IOException ioException) {
                    throw new RuntimeException(ioException);
                }
            } else if (listOfFiles[i].isDirectory()) {
                LibraryFolder folder = new LibraryFolder();
                folder.setName(listOfFiles[i].getName().toString())
                folder.setFolderId(rootFolder.getId());

                File[] listOfChildrenFiles = listOfFiles[i].listFiles();

                LibraryFolder resultFolder = client.createFolder(persistedLibraryId, folder);
                assert resultFolder.getName().equals(listOfFiles[i].getName());
                assert resultFolder.getId() != null;

                createFoldersAndFiles(listOfChildrenFiles, resultFolder, client, persistedLibraryId);
            }
        }
    }

    /**
     * Method that will get the list of jobs to show in the galaxy jobs tab
     */
    def getjobs(String userName, jobType = null) {
        JSONObject result = new JSONObject()
        JSONArray rows = new JSONArray()

        def jobResults = null
        def c = AsyncJob.createCriteria()
        if (StringUtils.isNotEmpty(jobType)) {
            jobResults = c {
                like("jobName", "${userName}%")
                eq("jobType", "${jobType}")
                ge("lastRunOn", new Date()-7)
                order("lastRunOn", "desc")
            }
        } else {
            jobResults = c {
                like("jobName", "${userName}%")
                or {
                    ne("jobType", "DataExport")
                    isNull("jobType")
                }
                ge("lastRunOn", new Date()-7)
                order("lastRunOn", "desc")
            }
        }

        def m = [:]
        def d
        for (jobResult in jobResults)	{
            m = [:]
            m["name"] = jobResult.jobName
            m["status"] = jobResult.jobStatus
            m["runTime"] = jobResult.jobStatusTime
            m["startDate"] = jobResult.lastRunOn
            m["viewerURL"] = jobResult.viewerURL
            m["altViewerURL"] = jobResult.altViewerURL
            m["jobInputsJson"] = new JSONObject(jobResult.jobInputsJson ?: "{}")
            d = getLatest(StatusOfExport.findAllByJobName(jobResult.jobName));
            if(!d.equals(null) ) {
                m["lastExportName"] = d.lastExportName;
                m["lastExportTime"] = d.lastExportTime.toString();
                m["exportStatus"] = d.jobStatus;
            }else{
                m["lastExportName"] = "Never Exported";
                m["lastExportTime"] = " ";
                m["exportStatus"] = " ";
            }
            rows.put(m)
        }

        result.put("success", true)
        result.put("totalCount", jobResults.size())
        result.put("jobs", rows)

        return result
    }

    private def getLatest(ArrayList<?> exports){

        switch (exports.size()){
            case 0:
                log.error("An error has occured while exporting to galaxy. The job name doesn't existe in the database");
                return null;
            case 1:
                return exports[0];
            default:
                def latest = exports[0];
                for(i in 1..exports.size()-1){
                    if(exports[i].id > latest.id){
                        latest = exports[i];
                    }
                }
                return latest;
        }
    }
}
