grails.cache.config = {
    cache {
        name 'FacetsIndexCache'
        eternal false
        timeToLiveSeconds(15 * 60)
        maxElementsInMemory 10
        maxElementsOnDisk 0
    }
}
