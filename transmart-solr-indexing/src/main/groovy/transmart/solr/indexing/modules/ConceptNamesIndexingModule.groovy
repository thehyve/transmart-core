package transmart.solr.indexing.modules

import com.google.common.collect.AbstractIterator
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterators
import com.google.common.collect.Multimap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import org.transmartproject.core.concept.ConceptFullName
import org.transmartproject.core.ontology.*
import transmart.solr.indexing.FacetsDocId
import transmart.solr.indexing.FacetsDocument
import transmart.solr.indexing.FacetsFieldDisplaySettings
import transmart.solr.indexing.FacetsIndexingModule
import transmart.solr.indexing.FacetsIndexingService
import transmart.solr.indexing.FolderConceptMappings

import static transmart.solr.indexing.FacetsIndexingService.*

@Component
class ConceptNamesIndexingModule implements FacetsIndexingModule {

    public static final String CONCEPT_DOC_TYPE = 'CONCEPT'

    final String name = 'concept_names'

    final Set<String> supportedDocumentTypes = ImmutableSet.of(CONCEPT_DOC_TYPE)

    private static final String FOLDER_CONCEPT_MAPPINGS_CACHE = 'FacetsIndexCache'
    private static final String UNMANAGED_TAGS_CACHE_KEY_PREFIX = 'unamanaged_tags_'
    private static final String ALL_CATEGORIES_KEY = 'all_categories'

    @Autowired
    private CacheManager cacheManager

    private Cache getCache() {
        cacheManager.getCache(FOLDER_CONCEPT_MAPPINGS_CACHE)
    }

    @Autowired
    private ConceptsResource conceptsResource

    @Autowired
    private OntologyTermTagsResource ontologyTermTagsResource

    @Autowired
    private FolderConceptMappings folderConceptMappings

    @Override
    Iterator<FacetsDocId> fetchAllIds(String type) {
        if (type != CONCEPT_DOC_TYPE) {
            return Iterators.emptyIterator()
        }

        List<OntologyTerm> toProcess = []
        toProcess += allCategories.values()

        new AbstractIterator<FacetsDocId>() {
            private List<String> lastQueryNames

            @Override
            protected FacetsDocId computeNext() {
                if (lastQueryNames) {
                    return new FacetsDocId(CONCEPT_DOC_TYPE, lastQueryNames.pop().getFullName())
                }
                if (toProcess.empty) {
                    return endOfData()
                }
                def el = toProcess.pop()
                lastQueryNames = el.getAllDescendantsForFacets()
                computeNext()
            }
        }
    }

    @Override
    Set<FacetsDocument> collectDocumentsWithIds(Set<FacetsDocId> docIds) {
        docIds.findAll { it.type == CONCEPT_DOC_TYPE }.collect { docId ->
            ConceptFullName fullName = new ConceptFullName(docId.id)
            def builder = FacetsDocument.newFieldValuesBuilder()

            builder[FIELD_CONCEPT_PATH] = docId.id
            builder[FIELD_FOLDER_ID] = folderConceptMappings.getFolderId(fullName)

            def tagsForFullName = getTagsForFullName(fullName)
            builder[FacetsIndexingService.FIELD_TEXT] = tagsForFullName?.collect { it.name }
            builder[FacetsIndexingService.FIELD_TEXT] = tagsForFullName?.collect { it.description }

            new FacetsDocument(
                    facetsDocId: docId,
                    fieldValues: builder.build())
        } as Set
    }

    private Map<String /* full name */, OntologyTerm> getAllCategories() {
        Map<String, OntologyTerm> result = cache.get(ALL_CATEGORIES_KEY, Map)
        if (result == null) {
            result = conceptsResource.allCategories.collectEntries {
                [it.fullName, it]
            }
            cache.put(ALL_CATEGORIES_KEY, result)
        }
        result
    }

    private List<OntologyTermTag> getTagsForFullName(ConceptFullName conceptFullName) {
        // there better not be more than one category root with the same full name
        // and they better be in the root. This is always the case in transmart
        OntologyTerm category
        for (int i = 1; i < conceptFullName.length; i++) {
            String categoryFullName = "\\${conceptFullName[0]}\\"
            if (allCategories.containsKey(categoryFullName)) {
                category = allCategories[categoryFullName]
            }
        }

        if (!category) {
            return
        }
        Multimap<String, OntologyTermTag> allTags = getAllTagsUnderCategory(category)
        allTags.get(conceptFullName.toString()) as List
    }

    private Multimap<String, OntologyTermTag> getAllTagsUnderCategory(OntologyTerm category) {
        String key = UNMANAGED_TAGS_CACHE_KEY_PREFIX + category.fullName
        Multimap<String, OntologyTermTag> result = cache.get(key, Multimap)
        if (result == null) {
            result = HashMultimap.create()
            ontologyTermTagsResource
                    .getTags(ImmutableSet.of(category), true)
                    .each { e ->
                e.value.each {
                    result.put(it.ontologyTermFullName, it)
                }
            }
            cache.put(key, result)
        }
        result
    }

    @Override
    FacetsFieldDisplaySettings getDisplaySettingsForIndex(String fieldName) {
        if (fieldName == FIELD_NAME_CONCEPT_PATH) {
            FIELD_CONCEPT_PATH
        } else if (fieldName == FIELD_FOLDER_ID) {
            FIELD_FOLDER_ID
        }
    }
}
