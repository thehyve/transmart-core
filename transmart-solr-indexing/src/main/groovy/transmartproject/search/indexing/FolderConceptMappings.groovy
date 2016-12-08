package org.transmartproject.search.indexing

import grails.plugin.cache.ehcache.GrailsEhcacheCacheManager
import net.sf.ehcache.Ehcache
import net.sf.ehcache.loader.CacheLoader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.core.concept.ConceptFullName
import org.transmartproject.search.browse.FolderStudyMappingView

@Component
class FolderConceptMappings {
    private static final String FOLDER_CONCEPT_MAPPINGS_CACHE = 'FacetsIndexCache'
    private static final String ROOT_MAPPINGS_KEY = 'root_mappings'
    private static final String ALL_MAPPINGS_KEY = 'all_mappings'

    @Autowired
    private GrailsEhcacheCacheManager grailsCacheManager

    private Ehcache getEhcache() {
        grailsCacheManager.getCache(FOLDER_CONCEPT_MAPPINGS_CACHE).nativeCache
    }

    private Map<ConceptFullName, Long /* folder id */>  getRootMappings() {
        ehcache.getWithLoader(ROOT_MAPPINGS_KEY, [
                load: { key ->
                    FolderStudyMappingView.findAllByRoot(true).collectEntries {
                        [new ConceptFullName(it.conceptPath), it.folderId]
                    }
                }] as CacheLoader, null).objectValue
    }

    private Map<Long, ConceptFullName>  getAllMappings() {
        ehcache.getWithLoader(ALL_MAPPINGS_KEY, [
                load: { key ->
                    FolderStudyMappingView.all.collectEntries {
                        [it.folderId, new ConceptFullName(it.conceptPath)]
                    }
                }] as CacheLoader, null).objectValue
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
