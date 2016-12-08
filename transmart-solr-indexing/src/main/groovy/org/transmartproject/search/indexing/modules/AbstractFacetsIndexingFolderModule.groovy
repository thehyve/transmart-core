package org.transmartproject.search.indexing.modules

import com.google.common.base.Objects
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterators
import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.search.browse.BioDataValuesView
import org.transmartproject.search.indexing.*

import static org.transmartproject.search.indexing.FacetsFieldImpl.create
import static org.transmartproject.search.indexing.FacetsIndexingService.FIELD_CONCEPT_PATH
import static org.transmartproject.search.indexing.FacetsIndexingService.FIELD_FOLDER_ID

/**
 * base class for browse folder indexers
 */
@Log4j
abstract class AbstractFacetsIndexingFolderModule<T> implements FacetsIndexingModule {

    public static final String FOLDER_DOC_TYPE = 'FOLDER'

    protected static final FacetsFieldImpl FIELD_TITLE            = create FacetsIndexingService.FIELD_NAME_TITLE
    protected static final FacetsFieldImpl FIELD_DESCRIPTION      = create 'description_t'
    protected static final FacetsFieldImpl FIELD_MEASUREMENT_TYPE = create 'measurement_type_s'
    protected static final FacetsFieldImpl FIELD_PLATFORM_NAME    = create 'platform_s'
    protected static final FacetsFieldImpl FIELD_VENDOR           = create 'vendor_s'
    protected static final FacetsFieldImpl FIELD_TECHNOLOGY       = create 'technology_s'
    protected static final FacetsFieldImpl FIELD_BIOMARKER_TYPE   = create 'biomarker_type_s'
    protected static final FacetsFieldImpl FIELD_DISEASE          = create 'disease_s'
    protected static final FacetsFieldImpl FIELD_GENE             = create 'gene_s'

    final Set<String> supportedDocumentTypes = ImmutableSet.of(docType)

    /* subclasses should add to this on the constructor */
    protected final Set<FacetsFieldImpl> allFields = [
            FacetsIndexingService.FIELD_TEXT,
            FacetsIndexingService.FIELD_FOLDER_ID,
            FacetsIndexingService.FIELD_SUBTYPE,
            FIELD_TITLE,
            FIELD_DESCRIPTION,
            FIELD_MEASUREMENT_TYPE,
            FIELD_PLATFORM_NAME,
            FIELD_VENDOR,
            FIELD_TECHNOLOGY,
            FIELD_BIOMARKER_TYPE,
            FIELD_DISEASE,
            FIELD_GENE
    ] as Set

    @Autowired
    private FolderConceptMappings folderConceptMappings

    @Lazy
    private Map<String, FacetsFieldDisplaySettings> indexDisplaySettings =
       allFields.collectEntries {
           [it.fieldName, it]
       }

    protected String getDocType() {
        FOLDER_DOC_TYPE
    }

    protected abstract Class<T> getDomainClass()

    protected abstract void doConvert(T obj, FacetsDocument.FieldValuesBuilder builder)

    private FacetsDocument convert(T obj) {
        def facetsDocId = docIdFromDbObject(obj)
        def builder = new FacetsDocument.FieldValuesBuilder()

        builder[FIELD_FOLDER_ID] = facetsDocId.id as Long
        builder[FIELD_CONCEPT_PATH] = folderConceptMappings.getConceptFullname(facetsDocId.id as Long)?.toString()

        doConvert obj, builder

        new FacetsDocument(
                facetsDocId: facetsDocId,
                fieldValues: builder.build())
    }

    protected FacetsDocId docIdFromDbObject(T dbObject) {
        String folderId = dbObject.identifier.replaceFirst(/\AFOL:/,'')
        new FacetsDocId(docType, folderId)
    }

    final protected List<BioDataValuesView> resolveUniqueIds(List<String> uniqueIds) {

        List<BioDataValuesView> results =
                BioDataValuesView.findAllByUniqueIdInList(uniqueIds)

        if (results.size() != uniqueIds.size()) {
            log.warn(
                    "Extra or insufficient values found for unique ids $uniqueIds: $results")
        }
        results
    }

    final protected List<BioDataValuesView> splitAndResolve(String pipeSeparatedUniqueIds) {
        if (!pipeSeparatedUniqueIds) {
            return null
        }

        /* right now we ignore the description */
        resolveUniqueIds(pipeSeparatedUniqueIds.split(/\|/) as List)
    }

    @Override
    Iterator<FacetsDocId> fetchAllIds(String type) {
        if (docType != type) {
            return Collections.emptyIterator()
        }

        domainClass.createCriteria().list {
            projections {
                property('identifier')
            }
        }.collect {
            new FacetsDocId(docType, it.replaceFirst(/\AFOL:/,''))
        }.iterator()
    }

    @Override
    Set<FacetsDocument> collectDocumentsWithIds(Set<FacetsDocId> docIds) {
        def tableIds = docIds
                .findAll { it.type == docType }
                .collect { "FOL:${it.id}".toString() }

        if (!tableIds) {
            return [] as Set
        }

        domainClass.withCriteria {
            'in'('identifier', tableIds as List)
        }.collect { convert it } as Set
    }


    @Override
    FacetsFieldDisplaySettings getDisplaySettingsForIndex(String fieldName) {
        indexDisplaySettings[fieldName]
    }


    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add('name', name)
                .add('supportedDocumentTypes', supportedDocumentTypes)
                .toString()
    }
}
