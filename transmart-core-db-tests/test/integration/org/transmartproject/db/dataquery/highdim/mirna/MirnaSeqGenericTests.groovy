package org.transmartproject.db.dataquery.highdim.mirna

import org.transmartproject.db.dataquery.highdim.HighDimensionGenericTests

class MirnaSeqGenericTests extends HighDimensionGenericTests {

    MirnaSeqGenericTests() {
        super('mirnaseq',
                ['rawIntensity', 'logIntensity', 'zscore'],
                ['probeId', 'mirnaId'],
                MirnaTestData)
    }
}
