package org.transmartproject.rest.serialization

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import org.apache.commons.lang.NotImplementedException
import org.transmartproject.core.IterableResult
import org.transmartproject.core.multidimquery.DefaultProperty
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Dimension.Density
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.Property
import org.transmartproject.core.multidimquery.dimensions.Order
import org.transmartproject.core.ontology.Study
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import spock.lang.Specification

import static org.transmartproject.rest.hypercubeProto.ObservationsProto.Type.DOUBLE
import static org.transmartproject.rest.hypercubeProto.ObservationsProto.Type.INT
import static org.transmartproject.rest.hypercubeProto.ObservationsProto.Type.STRING
import static org.transmartproject.rest.hypercubeProto.ObservationsProto.Type.TIMESTAMP


class HypercubeProtobufSerializerSpec extends Specification {
    final patientDim = new DenseDim('patient', fields: [id: Integer, trialId: String])
    final conceptDim = new DenseDim('concept', fields: [path: String, code: Double],
            packable: Dimension.Packable.NOT_PACKABLE)
    final dateDim = new SparseDim('date', type: Date)


    // todo: create mock values, test value serialization


    def getIndexedDims() { [conceptDim, patientDim]}
    def getInlineDims() { [dateDim] }
    def getAllDims() { indexedDims + inlineDims }

    HypercubeProtobufSerializer makeSerializer(Map args=[:], Hypercube cube) {
        def s = new HypercubeProtobufSerializer()
        args.putIfAbsent('pack', true)
        s.init(args, cube, new ByteArrayOutputStream())
        s
    }


    void testDimensions() {
        when:
        def cube = new MockHypercube(dimensions: indexedDims)
        HypercubeProtobufSerializer serializer = makeSerializer(cube, pack: false)

        then:
        cube.dimensions == indexedDims
        serializer.indexedDims == indexedDims
        serializer.inlineDims == []
        !serializer.packingEnabled
        serializer.packedDimension == null

        when:
        cube = new MockHypercube(dimensions: indexedDims + inlineDims)
        serializer = makeSerializer(cube, pack: false)

        then:
        cube.dimensions == indexedDims + inlineDims
        serializer.indexedDims == indexedDims
        serializer.inlineDims == inlineDims
        !serializer.packingEnabled

        when:
        serializer = makeSerializer(cube, pack: true)

        then:
        serializer.packingEnabled
        serializer.packedDimension == patientDim
        serializer.indexedDims == [conceptDim]
        serializer.inlineDims == [dateDim]

        when:
        serializer = makeSerializer(cube)

        then:
        serializer.packingEnabled
        serializer.packedDimension == patientDim
    }

    void 'test with non packable dimension as last sorted dimension'() {
        when:
        def serializer = makeSerializer(new MockHypercube(dimensions: [patientDim, conceptDim, dateDim]))

        then:
        !serializer.packingEnabled
        serializer.indexedDims == [patientDim, conceptDim]
        serializer.indexedDims == [patientDim, conceptDim]
    }

    void 'test with inline dimension in between indexed dimensions in sorting order'() {
        when:
        def serializer = makeSerializer(new MockHypercube(dimensions: [conceptDim, dateDim, patientDim]))

        then:
        !serializer.packingEnabled
        serializer.indexedDims == [conceptDim, patientDim]
        serializer.inlineDims == [dateDim]
    }

    void 'test with no sorted dims'() {
        when:
        def serializer = makeSerializer(new MockHypercube(dimensions: indexedDims + inlineDims, sorting: [dateDim]))

        then:
        !serializer.packingEnabled
        serializer.indexedDims == indexedDims
        serializer.inlineDims == inlineDims
    }


    void 'test DimensionDeclaration'() {
        when:
        def dimensions = indexedDims + inlineDims
        def serializer = makeSerializer(new MockHypercube(dimensions: allDims))
        List dimensionDecs = serializer.getDimensionsDefs()

        then:
        [dimensionDecs, allDims].transpose().each {
            ObservationsProto.DimensionDeclaration dec, Dimension dim ->
                assert dec.name == dim.name
                assert dec.inline == dim.density.isSparse
                assert dec.elements.fieldsList.empty
        }

        dimensionDecs[0..1]*.type.each { assert it == ObservationsProto.Type.OBJECT }
        dimensionDecs[2].type == TIMESTAMP

        dimensionDecs[1].packed == indexedDims[-1].packable.packable

        [dimensionDecs[0..1], indexedDims].transpose().each {
            ObservationsProto.DimensionDeclaration dec, Dimension dim ->
                [dec.fieldsList, dim.elementFields.values().asList()].transpose().each {
                    ObservationsProto.FieldDefinition field, Property prop ->
                        assert field.name == prop.name
                        assert prop.type in [
                                (TIMESTAMP): [Date],
                                (DOUBLE): [Double, Float],
                                (INT): [Long, Integer, Short],
                                (STRING): [String],
                            ][field.type]
                }
        }
    }

    void 'test Header'() {
        when:
        def serializer = makeSerializer(new MockHypercube(dimensions: allDims))
        def header = serializer.buildHeader()

        then:
        // not too much specifics to test here
        header.last
        header.dimensionDeclarationsList*.name == allDims*.name
    }



}


class DenseDim extends MockDimension {
    DenseDim(Map args=[:], String name) {
        super(args, name, Density.DENSE)
    }
}

class SparseDim extends MockDimension {
    SparseDim(Map args=[:], String name) {
        super(args, name, Density.SPARSE)
    }
}

class MockDimension implements Dimension {
    MockDimension(Map args = [:], String name, Density density) {
        this.name = name
        this.density = density
        packable = density.isDense ? Dimension.Packable.PACKABLE : Dimension.Packable.NOT_PACKABLE
        elementsSerializable = args.serializable != null ? args.serializable : density.isSparse

        elementType = args.type
        //fields = args.fields ?: null

        elementFields = args.fields?.collectEntries { nme, type ->
            [nme, new DefaultProperty(nme, type)]}

        'serializable type fields'.split().each { args.remove(it) }
        args.each { k, v -> this[k] = v }
    }

    String name

    Dimension.Size size = Dimension.Size.LARGE

    Dimension.Density density

    Dimension.Packable packable

    boolean determinesType = false

    IterableResult<Object> getElements(Collection<Study> studies) { throw new NotImplementedException() }
    List resolveElements(List elementKeys) { elementKeys }
    def resolveElement(elementKey) { elementKey }

    boolean elementsSerializable

    Class<? extends Serializable> elementType = null

//    List<String> fields
    Map elementFields
    ImmutableMap<String, Property> getElementFields() {
//        if(elementFields)
            return elementsSerializable ? null : ImmutableMap.copyOf(elementFields)
//
//        List<String> fieldNames = fields ?: values.find().metaClass.properties*.name
//        ImmutableMap.copyOf(fieldNames.collectEntries {
//            new Property() {
//                String getName() { it }
//                Class type = findPropertyType(it)
//                def get(element) { element[name] }
//            }
//        })
    }

//    private Class findPropertyType(String name) {
//        values.find {it[name] != null}[name].class
//    }
//
//    List values = null

    def asSerializable(element) {
        if(elementsSerializable) return element
        getElementFields().collectEntries { name, property -> [name: property.get(element)] }
    }
}

class MockHypercube implements Hypercube {

    MockHypercube(Map args) {
        dimensions = args.dimensions ?: []
        values = args.values ?: []

        //for(d in dimensions) if(d instanceof MockDimension) d.values = values

        for(dim in dimensions) {
            if(dim.density.isDense) {
                dimIndexes[dim] = values.collect(new HashSet()) { it[dim.name] } as List
            }
        }

        sorting = dimensions

        'dimensions values'.split().each { args.remove(it) }
        args.each { k, v -> this[k] = v }
    }

    List<Dimension> dimensions

    List<HypercubeValue> values

    Map<Dimension, List> dimIndexes = [:]

    static private void checkIndexed(Dimension dim) {
        assert dim.density.isDense, "Dimension $dim is not dense"
    }

    PeekingIterator<HypercubeValue> iterator() {
        Iterators.peekingIterator(values.iterator())
    }

    ImmutableList<Dimension> getDimensions() {
        ImmutableList.copyOf(dimensions)
    }

    ImmutableList<Object> dimensionElements(Dimension dim) {
        checkIndexed(dim)
        ImmutableList.copyOf(dimIndexes[dim])
    }

    List sorting

    ImmutableMap<Dimension, Order> getSorting() {
        ImmutableMap.copyOf(sorting.collectEntries { [it, Order.ASC] })
    }

    Object dimensionElement(Dimension dim, Integer idx) {
        dimIndexes[dim][idx]
    }

    Object dimensionElementKey(Dimension dim, Integer idx) { throw new NotImplementedException() }

    final boolean dimensionsPreloadable = true
    void preloadDimensions() {}
    final boolean dimensionsPreloaded = true
    final boolean autoloadDimensions = true
    void setAutoloadDimensions(boolean autoload) {}
    void loadDimensions() {}
    void close() {
        for(d in dimensions) if(d instanceof MockDimension) d.values = null
    }
}