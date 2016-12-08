package org.transmartproject.search.indexing.modules

import org.springframework.stereotype.Component
import org.transmartproject.search.browse.BrowseProgramsView
import org.transmartproject.search.indexing.FacetsDocument
import org.transmartproject.search.indexing.FacetsFieldImpl
import org.transmartproject.search.indexing.FacetsIndexingService

import static org.transmartproject.search.indexing.FacetsFieldImpl.create as createFF
import static org.transmartproject.search.indexing.FacetsIndexingService.FIELD_SUBTYPE

/**
 * Indexes folder by looking at the view biomart_user.browse_programs_view
 */
@Component
class BrowseProgramsIndexingModule extends AbstractFacetsIndexingFolderModule<BrowseProgramsView> {

    public static final String STUDY_FOLDER_DOC_SUBTYPE = 'PROGRAM'

    private static final FacetsFieldImpl FIELD_OBSERVATION         = createFF 'observation_s'
    private static final FacetsFieldImpl FIELD_PATHWAY             = createFF 'pathway_s'
    private static final FacetsFieldImpl FIELD_THERAPEUTIC_DOMAIN  = createFF 'therapeutic_domain_s'
    private static final FacetsFieldImpl FIELD_PROGRAM_INSTITUTION = createFF 'program_institution_s'
    private static final FacetsFieldImpl FIELD_PROGRAM_TARGET      = createFF 'program_target_s'

    final Class<BrowseProgramsView> domainClass = BrowseProgramsView
    final String name = 'browse_programs'

    BrowseProgramsIndexingModule() {
        allFields << FIELD_OBSERVATION
        allFields << FIELD_PATHWAY
        allFields << FIELD_THERAPEUTIC_DOMAIN
        allFields << FIELD_PROGRAM_INSTITUTION
        allFields << FIELD_PROGRAM_TARGET
    }

    @Override
    protected void doConvert(BrowseProgramsView dbObject, FacetsDocument.FieldValuesBuilder builder) {
        builder[FIELD_SUBTYPE] = STUDY_FOLDER_DOC_SUBTYPE
        dbObject.with {
            builder[FIELD_TITLE]               = dbObject.title
            builder[FIELD_DESCRIPTION]         = dbObject.description
            builder[FIELD_DISEASE]             = splitAndResolve(dbObject.disease)
            builder[FIELD_GENE]                = splitAndResolve(dbObject.gene)

            builder[FIELD_OBSERVATION]         = splitAndResolve(dbObject.observation)
            builder[FIELD_PATHWAY]             = splitAndResolve(dbObject.pathway)
            builder[FIELD_THERAPEUTIC_DOMAIN]  = splitAndResolve(dbObject.therapeuticDomain)
            builder[FIELD_PROGRAM_INSTITUTION] = splitAndResolve(dbObject.institution)
            builder[FIELD_PROGRAM_TARGET]      = splitAndResolve(dbObject.target)
        }
    }
}
