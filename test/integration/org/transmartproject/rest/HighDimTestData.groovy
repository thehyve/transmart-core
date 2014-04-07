package org.transmartproject.rest

import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimHeader
import org.transmartproject.rest.protobuf.HighDimProtos.Row

class HighDimTestData {

    ConceptTestData conceptData = new ConceptTestData()
    MrnaTestData mrnaData
    AcghTestData acghData

    void saveAll() {
        conceptData.saveAll()
        mrnaData?.saveAll()
        acghData?.saveAll()
    }

    static class HighDimResult {
        HighDimHeader header
        List<Row> rows = []

        List<Long> getAssayIds() {
            header.assayList*.assayId //order of assays in the actual data
        }
    }

}
