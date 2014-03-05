package blend4j.galaxy;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.HistoriesClient;
import com.github.jmchilton.blend4j.galaxy.LibrariesClient;
import com.github.jmchilton.blend4j.galaxy.SearchClient;
import com.github.jmchilton.blend4j.galaxy.SearchClient.SearchResponse;
import com.github.jmchilton.blend4j.galaxy.beans.*;
import com.github.jmchilton.blend4j.galaxy.beans.HistoryDataset.Source;
import com.sun.jersey.api.client.ClientResponse;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.util.Map;
import java.util.UUID;

public class SearchTest {
  private GalaxyInstance instance;
  private SearchClient client;

  @BeforeMethod
  public void init() {
    instance = TestGalaxyInstance.get();
    client = instance.getSearchClient();
  }
  
  @org.testng.annotations.Test
  public void testListJobs() throws InterruptedException {
    final String historyId = TestHelpers.getTestHistoryId(instance);
    final HistoriesClient historyClient = instance.getHistoriesClient();
    SearchResponse response = client.search("select * from job where state = 'new'");
    assert response.getResults().size() == 0;
    final File input1 = TestHelpers.getTestFile();
    TestHelpers.testUpload(instance, historyId, input1);
    response = client.search("select * from job where state = 'new'");
    assert response.getResults().size() == 1;
    final Map<String, Object> failedJob = response.getResults().get(0);
    TestHelpers.waitForHistory(instance.getHistoriesClient(), historyId);    
  }

  @org.testng.annotations.Test
  public void testLdda() throws InterruptedException {
    final File testFile = TestHelpers.getTestFile();
    final LibrariesClient client = IntegrationTest.getLibrariesClient();
    final Library testLibrary = IntegrationTest.createTestLibrary(client, "test-filesystem-paths" + UUID.randomUUID().toString());
    final LibraryContent rootFolder = client.getRootFolder(testLibrary.getId());
    final FileLibraryUpload upload = new FileLibraryUpload();
    upload.setName("MOOCOWFILE");
    upload.setFolderId(rootFolder.getId());
    upload.setFileType("tabular");
    upload.setFile(testFile);
    final ClientResponse uploadResponse = client.uploadFile(testLibrary.getId(), upload);
    IntegrationTest.assert200(uploadResponse);
    
    final String historyId = TestHelpers.getTestHistoryId(instance);
    final HistoryDataset hd = new HistoryDataset();
    hd.setSource(Source.LIBRARY);
    final String lddaId = client.getLibraryContents(testLibrary.getId()).get(1).getId();
    hd.setContent(lddaId);
    final HistoryDetails hda = instance.getHistoriesClient().createHistoryDataset(historyId, hd);
    final String id = instance.getHistoriesClient().showHistoryContents(historyId).get(0).getId();
    
    SearchResponse response;
    final String query = "select copied_from_ldda_id from hda where id = '" + id + "'";
    response = instance.getSearchClient().search(query);
    assert response.getResults().get(0).get("copied_from_ldda_id").equals(lddaId);
    TestHelpers.waitForHistory(instance.getHistoriesClient(), historyId);    
    
  }



}
