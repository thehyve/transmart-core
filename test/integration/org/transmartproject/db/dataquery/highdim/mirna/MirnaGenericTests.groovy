package org.transmartproject.db.dataquery.highdim.mirna

import org.transmartproject.db.dataquery.highdim.HighDimensionGenericTests

/**
 * Created by jan on 2/5/14.
 */
class MirnaGenericTests extends HighDimensionGenericTests {

    MirnaGenericTests() {
        super('mirna',
                ['rawIntensity', 'logIntensity', 'zscore'],
                ['probeId', 'mirnaId'],
                MirnaTestData)
    }
}
