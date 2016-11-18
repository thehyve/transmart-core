package org.transmartproject.search.indexing.modules

import com.google.common.collect.ImmutableSet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.search.browse.FolderStudyMappingView
import org.transmartproject.search.indexing.*

import static org.transmartproject.search.indexing.FacetsFieldImpl.create as createFF
import static org.transmartproject.search.indexing.FacetsIndexingService.FIELD_CONCEPT_PATH
import static org.transmartproject.search.indexing.FacetsIndexingService.FIELD_FOLDER_ID
import static org.transmartproject.search.indexing.modules.AbstractFacetsIndexingFolderModule.FOLDER_DOC_TYPE
import static org.transmartproject.search.indexing.modules.ConceptNamesIndexingModule.CONCEPT_DOC_TYPE

/**
 * Indexes data types under FOLDER: (if there is a folder link to the study)
 * or CONCEPT: documents.
 */
@Component
class DataTypesIndexingModule implements FacetsIndexingModule {

    final String name = 'data_types'

    final Set<String> supportedDocumentTypes = ImmutableSet.of(FOLDER_DOC_TYPE, CONCEPT_DOC_TYPE)

    private static final FacetsFieldImpl FIELD_DATA_TYPE = createFF 'data_type_s'

    @Autowired
    StudiesResource studyResource

    @Autowired
    HighDimensionResource highDimensionResource

    @Override
    Iterator<FacetsDocId> fetchAllIds(String type) {
        List<FolderStudyMappingView> linked = FolderStudyMappingView.findAllByRoot(true)

        if (type == FOLDER_DOC_TYPE) { /* those with links */
            linked*.folderId.collect {
                new FacetsDocId(FOLDER_DOC_TYPE, it as String)
            }.iterator()
        } else if (type == CONCEPT_DOC_TYPE) {
            studyResource.studySet*.ontologyTerm*.fullName.findAll {
                !(it in linked*.conceptPath)
            }.collect {
                new FacetsDocId(CONCEPT_DOC_TYPE, it)
            }.iterator()
        } else {
            Collections.emptyIterator()
        }
    }

    @Override
    Set<FacetsDocument> collectDocumentsWithIds(Set<FacetsDocId> docIds) {
        def studySet = studyResource.studySet
        docIds.collect { convert it, studySet }.findAll() as Set
    }

    private FacetsDocument convert(FacetsDocId docId, Set<Study> studySet) {
        def studyName, conceptPath, folderId
        if (docId.type == FOLDER_DOC_TYPE) {
            def res = FolderStudyMappingView.findAllByRootAndFolderId(true, docId.id as Long)
            if (!res) {
                return
            }
            studyName = res.find().uniqueId['EXP:'.size()..-1]
            conceptPath = res.find().conceptPath
            folderId = docId.id
        } else { /* CONCEPT */
            studyName = studySet.find { it.ontologyTerm.fullName == docId.id }?.id
            if (!studyName) {
                return
            }
            conceptPath = docId.id
            folderId = null
        }

        Set<HighDimensionDataTypeResource> res = highDimensionResource.getSubResourcesAssayMultiMap([
                highDimensionResource.createAssayConstraint(
                        AssayConstraint.TRIAL_NAME_CONSTRAINT, name: studyName)]).keySet()
        if (!res) {
            return
        }

        def builder = FacetsDocument.newFieldValuesBuilder()

        builder[FIELD_FOLDER_ID] = folderId
        builder[FIELD_CONCEPT_PATH] = conceptPath
        res.each {
            builder[FIELD_DATA_TYPE] = it.dataTypeDescription
        }

        new FacetsDocument(
                facetsDocId: docId,
                fieldValues: builder.build())
    }

    @Override
    FacetsFieldDisplaySettings getDisplaySettingsForIndex(String fieldName) {
        if (fieldName == FIELD_DATA_TYPE.fieldName) {
            FIELD_DATA_TYPE
        }
    }
}
