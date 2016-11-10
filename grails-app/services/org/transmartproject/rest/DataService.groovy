package org.transmartproject.rest

import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.clinical.MultidimensionalDataResourceService
import org.transmartproject.db.dataquery2.Hypercube
import org.transmartproject.rest.protobuf.ObservationsSerializer

@Transactional
class DataService {

    @Autowired
    MultidimensionalDataResourceService queryResource

    def writeData(/*Query can also be just a Map*/query, String format = "json", OutputStream out) {
        if (!format.equalsIgnoreCase("json")) {
            throw new UnsupportedEncodingException("Serialization format ${format} is not supported")
        }
        Hypercube result = queryResource.doQuery query
        ObservationsSerializer builder = new ObservationsSerializer(result)
        builder.writeTo(out, format)
    }
}
