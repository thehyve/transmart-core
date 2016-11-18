package org.transmartproject.search.indexing.modules

import com.google.common.collect.AbstractIterator
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Multimap
import grails.plugin.cache.ehcache.GrailsEhcacheCacheManager
import net.sf.ehcache.Ehcache
import net.sf.ehcache.loader.CacheLoader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.concept.ConceptFullName
import org.transmartproject.core.ontology.*
import org.transmartproject.search.indexing.*

import static org.transmartproject.search.indexing.FacetsIndexingService.*

@Component
class ConceptNamesIndexingModule implements FacetsIndexingModule {

    public static final String CONCEPT_DOC_TYPE = 'CONCEPT'

    final String name = 'concept_names'

    final Set<String> supportedDocumentTypes = ImmutableSet.of(CONCEPT_DOC_TYPE)

    private static final String FOLDER_CONCEPT_MAPPINGS_CACHE = 'FacetsIndexCache'
    private static final String UNMANAGED_TAGS_CACHE_KEY_PREFIX = 'unamanaged_tags_'
    private static final String ALL_CATEGORIES_KEY = 'all_categories'

    @Autowired
    private GrailsEhcacheCacheManager grailsCacheManager

    private Ehcache getEhcache() {
        grailsCacheManager.getCache(FOLDER_CONCEPT_MAPPINGS_CACHE).nativeCache
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
            return [] as Set
        }

        List<OntologyTerm> toProcess = []
        toProcess += allCategories.values()

        new AbstractIterator<FacetsDocId>() {
            @Override
            protected FacetsDocId computeNext() {
                if (toProcess.empty) {
                    return endOfData()
                }
                def el = toProcess.pop()
                toProcess += el.children
                new FacetsDocId(CONCEPT_DOC_TYPE, el.fullName)
            }
        }
    }

    @Override
    Set<FacetsDocument> collectDocumentsWithIds(Set<FacetsDocId> docIds) {
        docIds.findAll { it.type == CONCEPT_DOC_TYPE }. collect { docId ->
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
        ehcache.getWithLoader(ALL_CATEGORIES_KEY, [
                load: { key ->
                    conceptsResource.allCategories.collectEntries {
                        [it.fullName, it]
                    }
                }] as CacheLoader, null).objectValue
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
        ehcache.getWithLoader(UNMANAGED_TAGS_CACHE_KEY_PREFIX + category.fullName, [
                load: { dummy ->
                    def res = HashMultimap.create()
                    ontologyTermTagsResource.getTags(ImmutableSet.of(category), true)
                            .each { e -> e.value.each { res.put(e.key.fullName, it) } }
                    res
                }] as CacheLoader, null).objectValue
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
