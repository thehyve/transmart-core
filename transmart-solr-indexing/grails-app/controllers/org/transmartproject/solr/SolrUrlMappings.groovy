package org.transmartproject.solr


class SolrUrlMappings {

    static mappings = {
        "/indexing/fullReindex"(controller: 'indexing', action: 'fullReindex')
        "/indexing/clearQueryingCaches"(controller: 'indexing', action: 'clearQueryingCaches')
        "/facetsSearch/getFilterCategories"(controller: 'facetsSearch', action: 'getFilterCategories')
        "/facetsSearch/getSearchCategories"(controller: 'facetsSearch', action: 'getSearchCategories')
        "/facetsSearch/autocomplete"(controller: 'facetsSearch', action: 'autocomplete')
        "/facetsSearch/getFacetResults"(controller: 'facetsSearch', action: 'getFacetResults')
    }

}
