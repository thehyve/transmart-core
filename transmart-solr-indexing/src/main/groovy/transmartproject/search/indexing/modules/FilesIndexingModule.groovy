package org.transmartproject.search.indexing.modules

import com.google.common.collect.ImmutableSet
import grails.util.Holders
import groovy.util.logging.Log4j
import org.springframework.stereotype.Component
import org.transmartproject.browse.fm.FmFile
import org.transmartproject.search.indexing.*

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
        String folderUniqueId = fmFile.folder.uniqueId /* typically FOL:<folder id> */
        if (folderUniqueId =~ /\AFOL:/) {
            Long folderId = folderUniqueId.replaceFirst(/\AFOL:/, '') as Long
            builder[FIELD_FOLDER_ID] = folderId
        } else {
            log.warn("Unexpected unique id: $folderUniqueId")
        }

        new FacetsDocument(
                facetsDocId: new FacetsDocId(FILE_DOC_TYPE, fmFile.id.toString() /* not fmFile.uniqueId! */),
                fieldValues: builder.build(),
                file: new File(filestoreDirectory + File.separator +
                        fmFile.filestoreLocation + File.separator + fmFile.filestoreName))
    }

    @Override
    FacetsFieldDisplaySettings getDisplaySettingsForIndex(String fieldName) {
        if (fieldName == FIELD_FILENAME.fieldName) {
            FIELD_FILENAME
        }
    }
}
