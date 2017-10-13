package org.transmartproject.solr.modules

import org.springframework.stereotype.Component
import org.transmartproject.solr.FacetsFieldImpl
import org.transmartproject.solr.BrowseAssaysView
import org.transmartproject.solr.FacetsDocument

import static FacetsFieldImpl.create as createFF
import static org.transmartproject.solr.FacetsIndexingService.FIELD_SUBTYPE

/**
 * Indexes folder by looking at the view biomart_user.browse_folders_view
 */
@Component
class BrowseAssaysIndexingModule extends AbstractFacetsIndexingFolderModule<BrowseAssaysView> {

    public static final String ASSAY_FOLDER_DOC_SUBTYPE = 'ASSAY'

    private static final FacetsFieldImpl FIELD_MIRNA = createFF 'mirna_s'

    final Class<BrowseAssaysView> domainClass = BrowseAssaysView
    final String name = 'browse_assays'

    BrowseAssaysIndexingModule() {
        allFields << FIELD_MIRNA
    }

    @Override
    protected void doConvert(BrowseAssaysView dbObject, FacetsDocument.FieldValuesBuilder builder) {
        builder[FIELD_SUBTYPE] = ASSAY_FOLDER_DOC_SUBTYPE
        dbObject.with {
            builder[FIELD_TITLE] = dbObject.title
            builder[FIELD_DESCRIPTION] = dbObject.description
            builder[FIELD_MEASUREMENT_TYPE] = dbObject.measurementType?.split(/\|/)
            builder[FIELD_PLATFORM_NAME] = dbObject.platformName?.split(/\|/)
            builder[FIELD_VENDOR] = dbObject.vendor?.split(/\|/)
            builder[FIELD_TECHNOLOGY] = dbObject.technology?.split(/\|/)
            builder[FIELD_GENE] = splitAndResolve(dbObject.gene)
            builder[FIELD_MIRNA] = splitAndResolve(dbObject.mirna)
            builder[FIELD_BIOMARKER_TYPE] = splitAndResolve(dbObject.biomarkerType)
        }
    }
}
