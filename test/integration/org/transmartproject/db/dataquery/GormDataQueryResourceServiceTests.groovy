package org.transmartproject.db.dataquery

import org.transmartproject.db.highdim.HighDimTestData
import org.transmartproject.db.querytool.QueryResultData

@Mixin(HighDimTestData)
@Mixin(QueryResultData)
class GormDataQueryResourceServiceTests extends DataQueryResourceServiceTests {

    def dataQueryResourceService

    @Override
    void setUp() {
        testedService = dataQueryResourceService
        super.setUp()
    }
}
