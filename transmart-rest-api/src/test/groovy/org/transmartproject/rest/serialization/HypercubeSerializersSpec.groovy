package org.transmartproject.rest.serialization

import groovy.json.JsonSlurper
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.serialization.HypercubeJsonSerializer
import org.transmartproject.db.util.PeekingIteratorImpl
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import spock.lang.Specification

class HypercubeSerializersSpec extends Specification {

    def date = new Date(0)

    def hypercube = Mock(Hypercube)

    void setup() {
        def dim1 = Mock(Dimension)
        dim1.name >> 'test dim 1'
        dim1.elementsSerializable >> true
        dim1.elementType >> String
        dim1.density >> Dimension.Density.DENSE
        hypercube.dimensions >> [dim1]
        hypercube.dimensionElements(dim1) >> ['test dim 1 element 1']
        HypercubeValue hv1 = Mock(HypercubeValue)
        hv1.value >> date
        hv1.getDimElementIndex(dim1) >> 0
        hypercube.iterator() >> PeekingIteratorImpl.getPeekingIterator([
                hv1
        ].iterator())
    }

    void 'test json has date formatted as ISO string'() {
        def out = new ByteArrayOutputStream()

        when:
        new HypercubeJsonSerializer(hypercube, out).write()
        def json = new JsonSlurper().parse(new ByteArrayInputStream(out.toByteArray()), 'UTF-8')

        then:
        json
        json.cells
        json.cells[0].stringValue == '1970-01-01T00:00:00Z'
    }

    void 'test protobuf has date formatted as ISO string'() {
        def out = new ByteArrayOutputStream()

        when:
        new HypercubeProtobufSerializer(hypercube, out).write()
        def stream = new ByteArrayInputStream(out.toByteArray())
        ObservationsProto.Header.parseDelimitedFrom(stream)
        ObservationsProto.Cell cell = ObservationsProto.Cell.parseDelimitedFrom(stream)

        then:
        cell
        cell.stringValue == '1970-01-01T00:00:00Z'
    }

}
