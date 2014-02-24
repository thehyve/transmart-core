package org.transmartproject.db.dataquery.highdim.mirna

import org.transmartproject.db.dataquery.highdim.HighDimensionGenericTests

class MirnaQpcrGenericTests extends HighDimensionGenericTests {

    MirnaQpcrGenericTests() {
        super('mirnaqpcr',
                ['rawIntensity', 'logIntensity', 'zscore'],
                ['probeId', 'mirnaId'],
                MirnaTestData)
    }
}
