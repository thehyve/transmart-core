package org.transmartproject.solr

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import groovy.util.logging.Log4j
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.request.LukeRequest
import org.apache.solr.client.solrj.response.LukeResponse
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.luke.FieldFlag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.transmartproject.solr.modules.AbstractFacetsIndexingFolderModule

import static org.apache.solr.client.solrj.response.LukeResponse.FieldInfo.parseFlags
import static FacetsFieldImpl.getDefaultDisplayName

@Component
@Log4j
class FacetsQueryingService {

    private static final String ALL_DISPLAY_SETTINGS_CACHE_KEY = 'allDisplaySettings'
    private static final String TOP_TERMS_CACHE_KEY_PREFIX = 'topTerms'

    private static final String FACETS_QUERYING_SERVICE_CACHE = 'FacetsIndexCache'

    @Autowired
    private List<FacetsIndexingModule> modules

    @Autowired
    private SolrFacetsCore server

    @Autowired
    private CacheManager cacheManager

    private Cache getCache() {
        cacheManager.getCache(FACETS_QUERYING_SERVICE_CACHE)
    }

    private static final Set<String> BLACKLISTED_FIELD_NAMES =
            ImmutableSet.of(
                    FacetsIndexingService.FIELD_NAME_ID,
                    FacetsIndexingService.FIELD_NAME_TYPE,
                    FacetsIndexingService.FIELD_NAME_FILE_PATH,
                    FacetsIndexingService.FIELD_NAME_FOLDER_ID,
                    FacetsIndexingService.FIELD_NAME__VERSION_,
                    AbstractFacetsIndexingFolderModule.FOLDER_DOC_TYPE
            )

    void clearCaches() {
        cache.clear()
    }

    LinkedHashMap<String, SortedSet<TermCount>> getTopTerms(String requiredField) {
        String key = TOP_TERMS_CACHE_KEY_PREFIX + '_' + requiredField
        LinkedHashMap<String, SortedSet<TermCount>> result = cache.get(key, LinkedHashMap)
        if (result == null) {
            result = fetchTopTerms(requiredField)
            cache.put(key, result)
        }
        result
    }

    LinkedHashMap<String, FacetsFieldDisplaySettings> getAllDisplaySettings() {
        LinkedHashMap<String, FacetsFieldDisplaySettings> result = cache
                .get(ALL_DISPLAY_SETTINGS_CACHE_KEY, LinkedHashMap)
        if (result == null) {
            result = fetchAllDisplaySettings()
            cache.put(ALL_DISPLAY_SETTINGS_CACHE_KEY, result)
        }
        result
    }

    List<String> getAllFacetFields() {
        // only non-hidden _s fields
        allDisplaySettings.findAll { e ->
            e.key =~ /_s\z/ && !e.value.hideFromListings
        }
        .sort { it.value }
        .collect { it.key }
    }

    // leak solr api details, no point abstracting it
    QueryResponse query(SolrQuery query) {
        server.query(query)
    }

    LinkedHashMap<String, SortedSet<TermCount>> parseFacetCounts(QueryResponse response) {
        response.facetFields.collectEntries {
            [it.name,
             Sets.newTreeSet(it.values.collect {
                 v -> new TermCount(term: v.name, count: v.count)
             })]
        }
    }

    private LinkedHashMap<String, SortedSet<TermCount>> fetchTopTerms(String requiredField) {
        log.info('Going to calculate top terms')

        def q = new SolrQuery("${requiredField ?: '*'}:*")
        q.addFacetField(*allFacetFields)
        q.rows = 0
        parseFacetCounts(server.query(q))
    }

    private LinkedHashMap<String, FacetsFieldDisplaySettings> fetchAllDisplaySettings() {
        log.info('Going to determine all display settings')

        def req = new LukeRequest(numTerms: 0)
        LukeResponse lukeResponse = req.process(server.solrServer)

        lukeResponse.fieldInfo
                .entrySet()
                .findAll {
                    !(it.key in BLACKLISTED_FIELD_NAMES) &&
                            !(it.key =~ /_${FacetsFieldType.STRING_LOWERCASE.suffix}\z/) /* only used for searching */
                }
                .findAll { Map.Entry<String, LukeResponse.FieldInfo> e ->
                    parseFlags(e.value.schema).contains(FieldFlag.INDEXED)
                }
                .collect { [it.key, getDisplaySettingsForField(it.key)] }
                .sort { a, b -> a[1] <=> b[1] }
                .collectEntries()
    }

    private FacetsFieldDisplaySettings getDisplaySettingsForField(String fieldName) {
        List<FacetsFieldDisplaySettings> allDisplaySettings = modules
                .collect { it.getDisplaySettingsForIndex fieldName }
                .findAll()

        if (!allDisplaySettings) {
            log.debug("Using default settings for field $fieldName")
            return new SimpleFacetsFieldDisplaySettings(
                    displayName: getDefaultDisplayName(fieldName),
                    hideFromListings: false,
                    order: Ordered.LOWEST_PRECEDENCE,
            )
        }

        new SimpleFacetsFieldDisplaySettings(
                displayName: (allDisplaySettings*.displayName as Set).join(' / '),
                hideFromListings: allDisplaySettings*.hideFromListings.every(),
                order: allDisplaySettings*.order.min(),
        )
    }

}
