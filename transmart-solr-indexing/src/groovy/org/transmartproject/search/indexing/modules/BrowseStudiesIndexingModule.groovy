package org.transmartproject.search.indexing.modules

import org.springframework.stereotype.Component
import org.transmartproject.search.browse.BrowseStudiesView
import org.transmartproject.search.indexing.FacetsDocument
import org.transmartproject.search.indexing.FacetsFieldImpl
import org.transmartproject.search.indexing.FacetsIndexingService

import static org.transmartproject.search.indexing.FacetsFieldImpl.create as createFF
import static org.transmartproject.search.indexing.FacetsIndexingService.FIELD_SUBTYPE

/**
 * Indexes folder by looking at the view biomart_user.browse_studies_view
 */
@Component
class BrowseStudiesIndexingModule extends AbstractFacetsIndexingFolderModule<BrowseStudiesView> {

    public static final String STUDY_FOLDER_DOC_SUBTYPE = 'STUDY'

    private static final FacetsFieldImpl FIELD_DESIGN            = createFF 'design_s'
    private static final FacetsFieldImpl FIELD_ACCESS_TYPE       = createFF 'access_type_s'
    private static final FacetsFieldImpl FIELD_STUDY_INSTITUTION = createFF 'study_institution_s'
    private static final FacetsFieldImpl FIELD_COUNTRY           = createFF 'country_s'
    private static final FacetsFieldImpl FIELD_COMPOUND          = createFF 'compound_s'
    private static final FacetsFieldImpl FIELD_STUDY_OBJECTIVE   = createFF 'study_objective_s'
    private static final FacetsFieldImpl FIELD_SPECIES           = createFF 'species_s'
    private static final FacetsFieldImpl FIELD_STUDY_PHASE       = createFF 'study_phase_s'
    private static final FacetsFieldImpl FIELD_ACCESSION         = FacetsFieldImpl.create 'accession_s'

    final Class<BrowseStudiesView> domainClass = BrowseStudiesView
    final String name = 'browse_studies'

    BrowseStudiesIndexingModule() {
        allFields << FIELD_DESIGN
        allFields << FIELD_ACCESS_TYPE
        allFields << FIELD_STUDY_INSTITUTION
        allFields << FIELD_COUNTRY
        allFields << FIELD_COMPOUND
        allFields << FIELD_STUDY_OBJECTIVE
        allFields << FIELD_SPECIES
        allFields << FIELD_STUDY_PHASE
        allFields << FIELD_ACCESSION
    }

    @Override
    protected void doConvert(BrowseStudiesView dbObject, FacetsDocument.FieldValuesBuilder builder) {
        builder[FIELD_SUBTYPE] = STUDY_FOLDER_DOC_SUBTYPE
        dbObject.with {
            builder[FIELD_TITLE]             = dbObject.title
            builder[FIELD_DESCRIPTION]       = dbObject.description
            builder[FIELD_BIOMARKER_TYPE]    = splitAndResolve(dbObject.biomarkerType)
            builder[FIELD_DISEASE]           = splitAndResolve(dbObject.disease)

            builder[FIELD_ACCESSION]         = dbObject.accession
            builder[FIELD_DESIGN]            = splitAndResolve(dbObject.design)
            builder[FIELD_ACCESS_TYPE]       = splitAndResolve(dbObject.accessType)
            builder[FIELD_STUDY_INSTITUTION] = splitAndResolve(dbObject.institution)
            builder[FIELD_COUNTRY]           = splitAndResolve(dbObject.country)
            builder[FIELD_COMPOUND]          = splitAndResolve(dbObject.compound)
            builder[FIELD_STUDY_OBJECTIVE]   = splitAndResolve(dbObject.studyObjective)
            builder[FIELD_SPECIES]           = splitAndResolve(dbObject.organism)
            builder[FIELD_STUDY_PHASE]       = splitAndResolve(dbObject.studyPhase)
        }
    }
}
