package org.transmartproject.search.indexing.modules

import org.springframework.stereotype.Component
import org.transmartproject.search.browse.BrowseFoldersView
import org.transmartproject.search.indexing.FacetsDocument
import org.transmartproject.search.indexing.FacetsFieldImpl
import org.transmartproject.search.indexing.FacetsIndexingService

import static org.transmartproject.search.indexing.FacetsFieldImpl.create as createFF
import static org.transmartproject.search.indexing.FacetsIndexingService.FIELD_SUBTYPE

/**
 * Indexes fodler by looking at the view biomart_user.browse_folders_view
 */
@Component
class BrowsePlainFoldersIndexingModule extends AbstractFacetsIndexingFolderModule<BrowseFoldersView> {

    public static final String FOLDER_FOLDER_DOC_SUBTYPE = 'FOLDER'

    private static final FacetsFieldImpl FIELD_FILE_TYPE = createFF 'file_type_s'

    final Class<BrowseFoldersView> domainClass = BrowseFoldersView
    final String name = 'browse_plain_folders'

    BrowsePlainFoldersIndexingModule() {
        allFields << FIELD_FILE_TYPE
    }

    @Override
    protected void doConvert(BrowseFoldersView dbObject, FacetsDocument.FieldValuesBuilder builder) {
        builder[FIELD_SUBTYPE] = FOLDER_FOLDER_DOC_SUBTYPE
        dbObject.with {
            builder[FIELD_TITLE] = dbObject.title
            builder[FIELD_DESCRIPTION] = dbObject.description
            builder[FIELD_FILE_TYPE] = splitAndResolve(dbObject.fileType)
        }
    }
}
