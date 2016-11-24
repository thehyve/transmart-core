package transmartproject.search.indexing

import transmartproject.search.indexing.FacetsIndexingService
import transmartproject.search.indexing.FacetsQueryingService

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
