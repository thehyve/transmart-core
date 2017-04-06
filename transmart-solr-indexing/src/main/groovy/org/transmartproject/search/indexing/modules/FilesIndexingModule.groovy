package org.transmartproject.search.indexing.modules

import com.google.common.collect.ImmutableSet
import org.transmartproject.browse.fm.FmFile
import grails.util.Holders
import groovy.util.logging.Log4j
import org.springframework.stereotype.Component
import org.transmartproject.search.indexing.FacetsDocId
import org.transmartproject.search.indexing.FacetsDocument
import org.transmartproject.search.indexing.FacetsFieldDisplaySettings
import org.transmartproject.search.indexing.FacetsFieldImpl
import org.transmartproject.search.indexing.FacetsIndexingModule

import static org.transmartproject.search.indexing.FacetsFieldImpl.create as createFF
import static org.transmartproject.search.indexing.FacetsIndexingService.FIELD_FOLDER_ID

/**
 * Indexes file contents.
 */
@Component
@Log4j
class FilesIndexingModule implements FacetsIndexingModule {
    public static final String FILE_DOC_TYPE = 'FILE'

    public static final FacetsFieldImpl FIELD_FILENAME = createFF 'filename_t', true

    final String name = 'file_contents'

    final Set<String> supportedDocumentTypes = ImmutableSet.of(FILE_DOC_TYPE)

    private String getFilestoreDirectory() {
        Holders.config.com.recomdata.FmFolderService.filestoreDirectory
    }

    @Override
    Iterator<FacetsDocId> fetchAllIds(String type) {
        if (type != FILE_DOC_TYPE) {
            return Collections.emptyIterator()
        }

        FmFile.createCriteria().list {
            projections {
                property 'id'
            }
        }.collect {
            new FacetsDocId(FILE_DOC_TYPE, it.toString())
        }.iterator()
    }

    @Override
    Set<FacetsDocument> collectDocumentsWithIds(Set<FacetsDocId> docIds) {
        Set<FacetsDocId> relevantDocs = docIds.findAll {
            it.type == FILE_DOC_TYPE
        }
        if (!relevantDocs) {
            return [] as Set
        }

        FmFile.findByIdInList(relevantDocs.collect { it.id as Long })
                .collect { convert it }
    }

    private FacetsDocument convert(FmFile fmFile) {
        def builder = FacetsDocument.newFieldValuesBuilder()
        builder[FIELD_FILENAME] = fmFile.originalName
        String folderUniqueId = fmFile.folder?.uniqueId /* typically FOL:<folder id> */
        if (!folderUniqueId) {
            log.warn "No folder found for file $fmFile"
        } else if (folderUniqueId =~ /\AFOL:/) {
            Long folderId = folderUniqueId.replaceFirst(/\AFOL:/, '') as Long
            builder[FIELD_FOLDER_ID] = folderId
        } else {
            log.warn("Unexpected unique id: $folderUniqueId")
        }

        def file = new File(filestoreDirectory + File.separator +
                fmFile.filestoreLocation + File.separator + fmFile.filestoreName)
        if (!file.exists()) {
            log.warn "File $file not found (backing FmFile $fmFile)"
            file = null
        }

        new FacetsDocument(
                facetsDocId: new FacetsDocId(FILE_DOC_TYPE, fmFile.id.toString() /* not fmFile.uniqueId! */),
                fieldValues: builder.build(),
                file: file)
    }

    @Override
    FacetsFieldDisplaySettings getDisplaySettingsForIndex(String fieldName) {
        if (fieldName == FIELD_FILENAME.fieldName) {
            FIELD_FILENAME
        }
    }
}
