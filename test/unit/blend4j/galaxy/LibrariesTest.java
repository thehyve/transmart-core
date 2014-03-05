package blend4j.galaxy;

import com.github.jmchilton.blend4j.galaxy.LibrariesClient;
import com.github.jmchilton.blend4j.galaxy.beans.*;
import com.sun.jersey.api.client.ClientResponse;
import org.testng.annotations.Test;

import java.io.File;
import java.util.UUID;

public class LibrariesTest {
  @Test
  public void testCreateFolder() {
    final LibrariesClient client = IntegrationTest.getLibrariesClient();
    final Library testLibrary = IntegrationTest.createTestLibrary(client, "test-filesystem-paths" + UUID.randomUUID().toString());
    final LibraryContent rootFolder = client.getRootFolder(testLibrary.getId());
    final LibraryFolder folder = new LibraryFolder();
    folder.setDescription("Folder Descriptions");
    folder.setName("Folder Name");
    folder.setFolderId(rootFolder.getId());

    final ClientResponse result = client.createFolderRequest(testLibrary.getId(), folder);
    assert result.getStatus() == 200 : result.getEntity(String.class);
    assert folder.getId() == null;

    final LibraryFolder resultFolder = client.createFolder(testLibrary.getId(), folder);
    assert resultFolder.getName().equals("Folder Name");
    assert resultFolder.getId() != null;
  }
  
  
  @Test
  public void testPathPaste() {
    final LibrariesClient client = IntegrationTest.getLibrariesClient();
    final Library testLibrary = IntegrationTest.createTestLibrary(client, "test-filesystem-paths" + UUID.randomUUID().toString());
    final LibraryContent rootFolder = client.getRootFolder(testLibrary.getId());
    final boolean composite = false;
    final boolean linkData = false;
    final FilesystemPathsLibraryUpload upload = new FilesystemPathsLibraryUpload(composite);
    upload.setName("MOOCOW");
    upload.setContent("test-data/visualization/phyloviz/");
    upload.setLinkData(linkData);
    upload.setFolderId(rootFolder.getId());
    upload.setFileType("tabular");
    final ClientResponse uploadResponse = client.uploadFileFromUrl(testLibrary.getId(), upload);
    IntegrationTest.assert200(uploadResponse);
  }

  @Test
  public void testFileUpload() {
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
  }
}
