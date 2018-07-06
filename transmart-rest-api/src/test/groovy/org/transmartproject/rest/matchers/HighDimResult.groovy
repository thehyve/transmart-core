package org.transmartproject.rest.matchers

import org.transmartproject.rest.protobuf.HighDimProtos

class HighDimResult {
    HighDimProtos.HighDimHeader header
    List<HighDimProtos.Row> rows = []

    List<Long> getAssayIds() {
        header.assayList*.assayId //order of assays in the actual data
    }
}