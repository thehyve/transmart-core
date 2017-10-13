package org.transmartproject.solr.modules

import org.springframework.stereotype.Component
import org.transmartproject.solr.BrowseAnalysesView
import org.transmartproject.solr.FacetsDocument

import static org.transmartproject.solr.FacetsIndexingService.FIELD_SUBTYPE

/**
 * Gets documents from biomart_user.browse_analyses_view
 */
@Component
class BrowseAnalysesIndexingModule extends AbstractFacetsIndexingFolderModule<BrowseAnalysesView> {

    public static final String ANALYSIS_FOLDER_DOC_SUBTYPE = 'ANALYSIS'

    final String name = 'browse_analyses'

    final Class<BrowseAnalysesView> domainClass = BrowseAnalysesView

    protected void doConvert(BrowseAnalysesView dbObject, FacetsDocument.FieldValuesBuilder builder) {
        builder[FIELD_SUBTYPE] = ANALYSIS_FOLDER_DOC_SUBTYPE
        dbObject.with {
            builder[FIELD_TITLE]            = dbObject.title
            builder[FIELD_DESCRIPTION]      = dbObject.description
            builder[FIELD_MEASUREMENT_TYPE] = dbObject.measurementType?.split(/\|/)
            builder[FIELD_PLATFORM_NAME]    = dbObject.platformName?.split(/\|/)
            builder[FIELD_VENDOR]           = dbObject.vendor?.split(/\|/)
            builder[FIELD_TECHNOLOGY]       = dbObject.technology?.split(/\|/)
        }
    }
}
