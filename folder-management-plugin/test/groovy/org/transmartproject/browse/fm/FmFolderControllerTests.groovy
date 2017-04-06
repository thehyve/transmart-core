package org.transmartproject.browse.fm

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Test

@TestFor(FmFolderController)
@Mock(FmFile)
class FmFolderControllerTests {

    @Test
    void basicDownload() {
        String originalFilename = 'test original Name 丈.pdf'
        Long fileSize = 2009
        def file = new FmFile(
                displayName: 'test display name',
                originalName: originalFilename,
                fileSize: fileSize,
        )
        assert file.save()
        controller.fmFolderService = new FmFolderService()
        controller.fmFolderService.metaClass.getFile = { FmFile f ->
            def bogusFile = new File('bogus')
            bogusFile.metaClass.newInputStream = { ->
                new ByteArrayInputStream('foobar'.getBytes('UTF-8'))
            }
            bogusFile
        }

        params.id = file.id
        controller.downloadFile()

        assert response.headers('Content-disposition').size() == 1
        assert response.header('Content-disposition').decodeURL() == "attachment; filename*=UTF-8''$originalFilename"
        assert response.header('Content-length') == fileSize as String
        assert response.text == 'foobar'
    }
}
