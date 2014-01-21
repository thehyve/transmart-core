package org.transmartproject.db.dataquery.highdim.metabolite

import org.junit.Test

class MetaboliteEndToEndRetrievalTest {
    MetaboliteTestData testData = new MetaboliteTestData()

    @Test
    void dataPersistenceTest() {
        testData.saveAll()

    }
}