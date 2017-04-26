package transmart.solr.indexing

import org.springframework.beans.factory.annotation.Autowired

class IndexingController {

    @Autowired
    FacetsQueryingService facetsQueryingService

    @Autowired
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
