package org.transmartproject.db.dataquery

import org.transmartproject.db.highdim.HighDimTestData
import org.transmartproject.db.querytool.QueryResultData;

@Mixin(HighDimTestData)
@Mixin(QueryResultData)
class NoGormDataQueryResourceServiceTests extends DataQueryResourceServiceTests {

    def dataQueryResourceNoGormService

    @Override
    void setUp() {
        testedService = dataQueryResourceNoGormService
        super.setUp()
    }
}
