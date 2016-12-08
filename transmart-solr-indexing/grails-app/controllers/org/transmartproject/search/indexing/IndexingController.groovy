package org.transmartproject.search.indexing

class IndexingController {

    FacetsQueryingService facetsQueryingService
    FacetsIndexingService facetsIndexingService

    def fullReindex() {
        facetsIndexingService.clearIndex()
        facetsIndexingService.fullIndex()
        facetsQueryingService.clearCaches()

        render 'OK'
    }

    def clearQueryingCaches() {
        facetsQueryingService.clearCaches()

        render 'OK'
    }
}
