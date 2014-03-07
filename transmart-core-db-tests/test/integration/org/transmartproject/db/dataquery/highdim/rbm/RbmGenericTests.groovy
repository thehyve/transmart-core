package org.transmartproject.db.dataquery.highdim.rbm

import org.transmartproject.db.dataquery.highdim.HighDimensionGenericTests

/**
 * Created by jan on 2/5/14.
 */
class RbmGenericTests extends HighDimensionGenericTests {

    RbmGenericTests() {
        super('rbm',
                ['value', 'zscore'],
                ['antigenName', 'uniprotName'],
                RbmTestData)
    }
}
