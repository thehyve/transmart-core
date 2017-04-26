package transmart.solr.indexing

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.stereotype.Component
import org.transmartproject.core.concept.ConceptFullName
import org.springframework.cache.CacheManager

@Component
class FolderConceptMappings {
    private static final String FOLDER_CONCEPT_MAPPINGS_CACHE = 'FacetsIndexCache'
    private static final String ROOT_MAPPINGS_KEY = 'root_mappings'
    private static final String ALL_MAPPINGS_KEY = 'all_mappings'

    @Autowired
    private CacheManager cacheManager

    private Cache getCache() {
        cacheManager.getCache(FOLDER_CONCEPT_MAPPINGS_CACHE)
    }

    private Map<ConceptFullName, Long /* folder id */>  getRootMappings() {
        Map<ConceptFullName, Long> result = cache.get(ROOT_MAPPINGS_KEY, Map)
        if (result == null) {
            result = FolderStudyMappingView.findAllByRoot(true).collectEntries {
                [new ConceptFullName(it.conceptPath), it.folderId]
            }
            cache.put(ROOT_MAPPINGS_KEY, result)
        }
        result
    }

    private Map<Long, ConceptFullName>  getAllMappings() {
        Map<Long, ConceptFullName> result = cache.get(ALL_MAPPINGS_KEY, Map)
        if (result == null) {
            result = FolderStudyMappingView.all.collectEntries {
                [it.folderId, new ConceptFullName(it.conceptPath)]
            }
            cache.put(ALL_MAPPINGS_KEY, result)
        }
        result
    }

    Long getFolderId(ConceptFullName path) {
        def folderId = rootMappings.get(path)
        if (folderId != null) {
            folderId
        } else if (path.length > 1) {
            getFolderId(path.parent)
        }
    }

    ConceptFullName getConceptFullname(Long folderId) {
        allMappings.get(folderId)
    }
}
