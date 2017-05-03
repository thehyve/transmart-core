package org.transmartproject.rest.serialization

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import org.apache.commons.lang.NotImplementedException
import org.transmartproject.core.IterableResult
import org.transmartproject.core.multidimquery.DefaultProperty
import org.transmartproject.core.multidimquery.IdentityProperty
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Dimension.Density
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.Property
import org.transmartproject.core.multidimquery.dimensions.Order
import org.transmartproject.core.ontology.Study
import org.transmartproject.rest.hypercubeProto.ObservationsProto
import org.transmartproject.rest.serialization.HypercubeProtobufSerializer.PackedCellBuilder
import org.transmartproject.rest.serialization.HypercubeProtobufSerializer as HPS
import spock.lang.Specification

import static org.transmartproject.rest.hypercubeProto.ObservationsProto.Type.DOUBLE
import static org.transmartproject.rest.hypercubeProto.ObservationsProto.Type.INT
import static org.transmartproject.rest.hypercubeProto.ObservationsProto.Type.STRING
import static org.transmartproject.rest.hypercubeProto.ObservationsProto.Type.TIMESTAMP

import static org.transmartproject.rest.serialization.DeserializationHelper.*


class HypercubeProtobufSerializerSpec extends Specification {
    final patientDim = new DenseDim('patient', fields: [id: Integer, trialId: String])
    final conceptDim = new DenseDim('concept', fields: [path: String, code: Double],
            packable: Dimension.Packable.NOT_PACKABLE)
    final dateDim = new SparseDim('date', type: Date)
    final visitDim = new DenseDim('visit', type: Integer, serializable: true)

    final anotherDim = new DenseDim('anotherDim', type: Integer, serializable: true)

    List<Map> getPatients() { [
            [id: 10, trialId: "patient_10"],
            [id: 11, trialId: "patient_11"],
            [id: 12, trialId: "patient_12"],
    ] }
    List<Map> getConcepts() { [
            [path: "foo", code: 1.1],
            [path: "bar", code: 1.2],
    ] }

    List<Map> getObservations() { [
            [visit: 0, patient: patients[0], concept: concepts[0], value: 1.2, date: new Date(100000)],
            // skipped: [visit: 0, patient: patients[1], concept: concepts[0], value: 1.5, date: new Date(110000)],
            [visit: 0, patient: patients[2], concept: concepts[0], value: 2.2, date: new Date(120000)],
            [visit: 0, patient: patients[0], concept: concepts[1], value: "FOO", date: new Date(105000)],
            [visit: 0, patient: patients[0], concept: concepts[1], value: "FOO2", date: new Date(106000)],
            [visit: 0, patient: patients[1], concept: concepts[1], value: null, date: new Date(115000)],
            [visit: 0, patient: patients[2], concept: concepts[1], value: "BAZ", date: null],

            [visit: 0, patient: null, concept: concepts[1], value: "QUUX", date: null],

            [visit: 1, patient: patients[0], concept: concepts[0], value: 1.3, date: new Date(200000)],
            [visit: 1, patient: patients[0], concept: concepts[0], value: 1.1, date: new Date(200000)],
            [visit: 1, patient: patients[1], concept: concepts[0], value: 2.1, date: new Date(210000)],
            [visit: 1, patient: patients[2], concept: concepts[0], value: 1.5, date: new Date(220000)],
            [visit: 1, patient: patients[2], concept: concepts[0], value: 1.7, date: new Date(220000)],
            [visit: 1, patient: patients[0], concept: concepts[1], value: null, date: new Date(205000)],
            [visit: 1, patient: patients[1], concept: concepts[1], value: "BAR", date: new Date(205000)],
            [visit: 1, patient: patients[2], concept: concepts[1], value: null, date: new Date(205000)],
    ] }

    List<List<Map>> getGroupedObservations() {
        observations.groupBy { [it.visit, it.concept] }.values().asList()
    }

    def groupsWithNulls = ImmutableList.of(1)
    def groupsWithMissing = ImmutableList.of(0)
    def groupsWithMulti = ImmutableList.of(20)

//    def immutate(collection) {
//        if(!(collection instanceof Collection)) return collection
//        assert collection instanceof List || collection instanceof Map
//        if(collection instanceof List) {
//            ImmutableList.copyOf( collection.collect { immutate(it) })
//        } else if(collection instanceof Map) {
//            ImmutableMap.copyOf( collection.collectEntries { k, v ->
//                [immutate(k), immutate(v)]
//            })
//        }
//    }

    // scalar test dimensions
    final stringDim = new SparseDim('string', type: String)
    final doubleDim = new SparseDim('double', type: Double)
    final intDim = new SparseDim('int', type: Long)
    final testStringDim = new SparseDim('testString', type: String, serializable: false, elementFields: [
            field: new IdentityProperty('field', String) {
                def get(element) { element == "__" ? null : element }
            }])


    def getIndexedDims() { [visitDim, conceptDim, patientDim] }
    def getInlineDims() { [dateDim] }
    def getAllDims() { indexedDims + inlineDims }

    def getDefaultSerializer() {
        makeSerializer(new MockHypercube(dimensions: allDims, values: observations))
    }

    def getDefaultPackedSerializer() {
        def s = defaultSerializer
        [s, s.packedCellBuilder]
    }

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
        serializer.indexedDims == indexedDims - patientDim
        serializer.inlineDims == [dateDim]

        when:
        serializer = makeSerializer(cube)

        then:
        serializer.packingEnabled
        serializer.packedDimension == patientDim

        when:
        serializer = makeSerializer(new MockHypercube(dimensions: [visitDim, conceptDim, patientDim],
                sorting: [visitDim, conceptDim]))

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
        def serializer = makeSerializer(new MockHypercube(dimensions: [conceptDim, dateDim, visitDim, patientDim]))

        then:
        !serializer.packingEnabled
        serializer.indexedDims == [conceptDim, visitDim, patientDim]
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
        def serializer = defaultSerializer
        List dimensionDecs = serializer.getDimensionsDefs()

        then:
        [dimensionDecs, allDims].transpose().each {
            ObservationsProto.DimensionDeclaration dec, Dimension dim ->
                assert dec.name == dim.name
                assert dec.inline == dim.density.isSparse
                assert dec.elements.fieldsList.empty
        }

        dimensionDecs[1..2]*.type.each { assert it == ObservationsProto.Type.OBJECT }
        dimensionDecs[3].type == TIMESTAMP

        dimensionDecs[2].packed == indexedDims[-1].packable.packable

        [dimensionDecs[1..2], indexedDims - visitDim].transpose().each {
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
        def serializer = defaultSerializer
        def header = serializer.buildHeader()

        then:
        // not too much specifics to test here
        !header.last
        header.dimensionDeclarationsList*.name == allDims*.name

        when:
        serializer = makeSerializer(new MockHypercube(dimensions: allDims, values: []))
        header = serializer.buildHeader()

        then:
        header.last
        header.dimensionDeclarationsList*.name == allDims*.name
    }

    void 'test buildValue'() {
        when:
        def serializer = defaultSerializer
        def value = serializer.buildValue(10)

        then:
        value.valueCase == ObservationsProto.Value.ValueCase.INTVALUE
        value.intValue == 10

        when:
        def date = new Date(1000)
        value = serializer.buildValue(date)

        then:
        value.valueCase == ObservationsProto.Value.ValueCase.TIMESTAMPVALUE
        value.timestampValue == 1000

        when:
        value = serializer.buildValue("hello")

        then:
        value.valueCase == ObservationsProto.Value.ValueCase.STRINGVALUE
        value.stringValue == "hello"

        when:
        value = serializer.buildValue(3.14)

        then:
        value.valueCase == ObservationsProto.Value.ValueCase.DOUBLEVALUE
        value.doubleValue == 3.14 as Double
    }

    void 'test buildDimensionElement'() {
        when:
        def serializer = makeSerializer(new MockHypercube(dimensions: allDims << stringDim))
        def stringElem = serializer.buildDimensionElement(stringDim, "hello world")
        def doubleElem = serializer.buildDimensionElement(doubleDim, 2.5)

        then:
        decodeDimensionElement(stringElem) == "hello world"
        decodeDimensionElement(doubleElem) == 2.5

        when:
        def patient = [id: 10, trialId: "patient_10"]
        def patientElem = serializer.buildDimensionElement(patientDim, patient)

        then:
        decodeDimensionElement(patientElem, patientDim.elementFields.keySet()) == patient

        when:
        patient = [id: 20]
        def partialElem = serializer.buildDimensionElement(patientDim, patient)

        then:
        decodeDimensionElement(partialElem, patientDim.elementFields.keySet()) == [id: 20, trialId: null]
    }

    void 'test createCell'() {
        when:
        HPS serializer = makeSerializer(new MockHypercube(dimensions: allDims, values: observations), pack: false)
        def cells = serializer.iterator.collect { serializer.createCell(it) }
        def cube = serializer.cube
        def decodedCells = cells.collect { decodeCell(it, cube.dimIndexes, inlineDims) }

        then:
        decodedCells*.value == observations*.value
        decodedCells*.patient == observations*.patient
        decodedCells*.concept == observations*.concept
        decodedCells*.date == observations*.date


        // The below does the work of decodeCell by hand. I'm leaving it in now since it's there and works, but if
        // the test need to change for some reason don't be afraid to remove this block.
        cells.collect {cellValue(it)} == observations*.value
        cells.collect { inlineDims(it)[0] } == observations*.date
        cells.collect {
            decodeIndexedDim(it.dimensionIndexesList[indexedDims.indexOf(patientDim)] as Integer,
                    cube.dimensionElements(patientDim))
        } == observations*.patient
        cells.collect {
            decodeIndexedDim(it.dimensionIndexesList[indexedDims.indexOf(conceptDim)] as Integer,
                    cube.dimensionElements(conceptDim))
        } == observations*.concept
        cells.collect {
            def elem = handleAbsents(it.inlineDimensionsList, it.absentInlineDimensionsList)[0]
            elem ? decodeDimensionElement(elem) : null
        } == observations*.date

        when:
        def lastFlags = cells*.last

        then:
        lastFlags[-1]
        lastFlags[0..-2].every { !it }

        cells*.error.every { !it }
    }

    void 'test buildElementFields'() {
        when:
        def serializer = defaultSerializer
        String name = "name"
        List elements = ["hello", "world", "out", "there"]
        Class type = String
        final mkcolumn = { ->
            serializer.buildElementFields(new IdentityProperty(name, type), elements)
        }
        def column = mkcolumn()


        // standard cases (no nulls) for each type

        then:
        parseFieldColumn(column) == elements

        when:
        elements = [1,2,3]
        type = Integer
        column = mkcolumn()

        then:
        parseFieldColumn(column) == elements

        when:
        type = Double
        column = mkcolumn()

        then:
        parseFieldColumn(column) == elements

        when:
        type = Date
        elements = [1,2,3,4].collect { new Date(it) }
        column = mkcolumn()

        then:
        parseFieldColumn(column) == elements


        // null cases: both when the element is null and when only the property is null

        when:
        elements = ["hello", null, "world"]
        type = String
        column = mkcolumn()

        then:
        parseFieldColumn(column, [2]) == elements

        when:
        elements = ["hello", "cruel", "__"]
        def testProperty = testStringDim.elementFields.field
        column = serializer.buildElementFields(testProperty, elements)

        then:
        parseFieldColumn(column) == ["hello", "cruel", null]

        when:
        elements = ["hello", null, "__"]
        column = serializer.buildElementFields(testProperty, elements)

        then:
        parseFieldColumn(column, [2]) == ["hello", null, null]

        serializer.buildElementFields(testProperty, []) == null
        serializer.buildElementFields(testProperty, [null, null]) == null
        serializer.buildElementFields(testProperty, ["__", "__"]) == null
    }

    void 'test buildDimensionElements'() {
        when:
        def serializer = defaultSerializer

        then:
        serializer.buildDimensionElements(stringDim, []).name == ""
        serializer.buildDimensionElements(stringDim, [], true).name == stringDim.name

        when:
        def elements = ["hello", "nice", "world"]
        def dimElems
        final mkElems = { dimElems = serializer.buildDimensionElements(testStringDim, elements) }
        mkElems()
        def fieldNames = testStringDim.elementFields.keySet()

        then:
        !dimElems.empty
        parseDimensionElements(dimElems, fieldNames)*.field == elements
        dimElems.scopeCase == ObservationsProto.DimensionElements.ScopeCase.SCOPE_NOT_SET

        when:
        elements = [null, null]
        mkElems()

        then:
        dimElems.empty
        parseDimensionElements(dimElems, fieldNames) == null

        when:
        elements = [null, "hoi"]
        mkElems()

        then:
        dimElems.absentElementIndicesList == [1]
        parseDimensionElements(dimElems, fieldNames)*.field == elements

        when:
        elements = ["__", "hoi"]
        mkElems()

        then:
        parseDimensionElements(dimElems, fieldNames)*.field == [null, "hoi"]
    }

    void 'test buildFooter'() {
        when:
        def serializer = defaultSerializer
        def cube = serializer.cube
        def footer = serializer.buildFooter()

        then:
        [footer.dimensionList, cube.dimIndexes.entrySet().asList()].transpose().every {
            ObservationsProto.DimensionElements elems, Map.Entry<Dimension, List> entry ->
            parseDimensionElements(elems, entry.key.elementFields?.keySet()) == entry.value
        }
        !footer.error
    }

    void 'test indices'() {
        expect:
        def serializer = makeSerializer(new MockHypercube(dimensions: allDims, values: observations), pack: false)
        def packer = serializer.packedCellBuilder
        def cube = serializer.cube
        [cube.toList(), observations].transpose().each { HypercubeValue hv, obs ->
            assert packer.indices(hv) == indexedDims.collect {
                def idx = cube.dimIndexes[it].indexOf(obs[it.name]); idx == -1 ? null : idx
            }
        }
    }

    void 'test sameIndices'() {
        when:
        def serializer = makeSerializer(new MockHypercube(dimensions: allDims, values: observations), pack: false)
        def packer = serializer.packedCellBuilder
        MockHypercube cube = serializer.cube

        then:
        cube.values.each {
            assert packer.sameIndices(it, packer.indices(it))
        }
    }


    void 'test compatibleValueType'() {
        when:
        def (serializer, PackedCellBuilder packer) = defaultPackedSerializer
        def stringRef = new Reference(String)
        def nullRef = new Reference()
        def doubleRef = new Reference(Double)

        then:
        packer.compatibleValueType(String, stringRef)
        packer.compatibleValueType(null, stringRef)
        packer.compatibleValueType(null, nullRef)
        !packer.compatibleValueType(Double, stringRef)
        !packer.compatibleValueType(Number, doubleRef)

        packer.compatibleValueType(Double, nullRef)
        !packer.compatibleValueType(String, nullRef)
    }

    def <T> List<T> iterateWhile(Closure<Boolean> condition, Closure<T> callable) {
        List res = []
        while(condition.call()) {
            res << callable.call()
        }
        res
    }

    void 'test nextPack'() {
        when:
        def (serializer, PackedCellBuilder packer) = defaultPackedSerializer
        def iterator = serializer.iterator
        def pack_type = iterateWhile({iterator.hasNext()}) { packer.nextPack() }
        def packs = pack_type*.aValue

        then:
        packs.size() == 4
        pack_type.each { pair ->
            def group = pair.aValue, type = pair.bValue
            group.each {
                assert it.value?.class in [type, null]
            }
        }

        when:
        // group by visit and concept, then within each group move the patient==null cases to the front
        def groupedObs = observations.groupBy { [it.visit, it.concept] }.values() as List

        then:
        groupedObs == packs.collectNested { it.val }

        when: "Test nextPack when all values are null"
        serializer = makeSerializer(new MockHypercube(dimensions: [visitDim], values:
                (1..5).collect { [value: null, visit: it] }))
        packer = serializer.packedCellBuilder
        iterator = serializer.iterator()
        pack_type = packer.nextPack()
        def pack = pack_type.aValue
        def type = pack_type.bValue

        then:
        pack*.value == [null]*5
        type == null
    }

    void 'test groupSamples'() { // also tests addGroup
        when:
        def group = { List vals ->
            def values = (1..vals.max()).collect { [value: it, anotherDim: 1, visit: it] }
            values += [vals.indices, vals].transpose().collect { ind, patientId ->
                [value: ind, anotherDim: 2, visit: patientId ]}
            def cube = new MockHypercube(dimensions: [anotherDim, visitDim], values: values)
            def serializer = makeSerializer(cube)
            assert serializer.packedDimension == visitDim
            serializer.packedCellBuilder.nextPack()  // so the serializer knows which visits exist
            def hValues = serializer.packedCellBuilder.nextPack().aValue
            List<List<MockValue>> groups = serializer.packedCellBuilder.groupSamples(hValues)
            return groups.collect { it*.getAt(visitDim) }
        }

        def values = [
                [null, null], [1,1],[2,2],[3,3],[4,4],[5,5]
        ]
        def groups = group(values.flatten())

        then:
        groups == values

        when:
        def nonnulls = [[1,1], [2], [3], [4,4,4,4]]
        groups = group([null, null] + nonnulls.flatten() + [null])

        then:
        groups[0] == [null]*3
        groups[1..4] == nonnulls

        when:
        groups = group(nonnulls.flatten())

        then:
        groups[0] == []
        groups[1..4] == nonnulls

        when:
        values = [null, 1,2,3,4]
        groups = group(values * 3)

        then:
        groups == values.collect { [it]*3 }

        when:
        groups = group([5])

        then:
        groups == [[], [], [], [], [], [5]]
    }

    void 'test firstNestedElement'() {
        when:
        def (serializer, PackedCellBuilder packer) = defaultPackedSerializer

        then:
        packer.firstNestedElement([[1]]) == 1
        packer.firstNestedElement([[], [], [], ['a', 'b', 'c']]) == 'a'
        packer.firstNestedElement([['a', 'b'], [], [3]]) == 'a'
    }

    void 'test getMode'() {
        when:
        def (serializer, PackedCellBuilder packer) = defaultPackedSerializer
        def values = { List<List<Integer>> vals ->
            vals.collectNested {
                new MockValue([int: it, value: 42], serializer.cube)
            }
        }

        then:
        packer.getMode(values([[1,1,1], [1,1,1], [], [1]]), intDim) == packer.PERPACK
        packer.getMode(values([[1]]), intDim) == packer.PERPACK
        packer.getMode(values([[], [], [1]]), intDim) == packer.PERPACK
        packer.getMode(values([[1,1,1], [2], [2,2,2]]), intDim) == packer.PERPACKELEMENT
        packer.getMode(values([[1,1,1], [], [1], [5], [1, 1]]), intDim) == packer.PERPACKELEMENT
        packer.getMode(values([[1,2]]), intDim) == packer.PEROBSERVATION
        packer.getMode(values([[], [1], [1, 1], [1, 1, 1, 2], [1, 1, 1], []]), intDim) == packer.PEROBSERVATION
        packer.getMode(values([[1, 1], [3, 4, 5, 1, 2], [1, 2, 1], [40]]), intDim) == packer.PEROBSERVATION
    }

    void 'test inlineDimension'() {
        when:
        def (HPS serializer, PackedCellBuilder packer) = defaultPackedSerializer
        double currval = 1.5
        def values = { List<List<Integer>> vals ->
            vals.collectNested {
                new MockValue([int: it, value: currval++], serializer.cube)
            }
        }

        def dimElements = packer.inlineDimension(values([[1]]), intDim)

        then:
        dimElements.name == ''
        dimElements.scopeCase == ObservationsProto.DimensionElements.ScopeCase.PERPACKEDCELL
        dimElements.fieldsCount == 1
        dimElements.fieldsList[0].intValueList == [1]
        dimElements.absentFieldColumnIndicesCount == 0
        dimElements.absentElementIndicesList == []
        dimElements.empty == false

        when:
        dimElements = packer.inlineDimension(values([[null], [1, 2], [3, 4]]), intDim)

        then:
        dimElements.name == ''
        dimElements.scopeCase == ObservationsProto.DimensionElements.ScopeCase.PERSAMPLE
        dimElements.fieldsCount == 1
        dimElements.fieldsList[0].intValueList == [1, 2, 3, 4]
        dimElements.absentFieldColumnIndicesCount == 0
        dimElements.absentElementIndicesList == [1]
        dimElements.empty == false

        when:
        dimElements = packer.inlineDimension(values([[1], [2, 2]]), intDim)

        then:
        dimElements.name == ''
        dimElements.scopeCase == ObservationsProto.DimensionElements.ScopeCase.SCOPE_NOT_SET
        dimElements.fieldsCount == 1
        dimElements.fieldsList[0].intValueList == [1, 2]
        dimElements.absentFieldColumnIndicesCount == 0
        dimElements.absentElementIndicesList == []
        dimElements.empty == false
    }

    void 'test equal'() {
        expect:
        HypercubeProtobufSerializer.equal(null, null)
        !HypercubeProtobufSerializer.equal(null, 0)
        HypercubeProtobufSerializer.equal(2, 2)
        !HypercubeProtobufSerializer.equal(2, "2")
        !HypercubeProtobufSerializer.equal(4, null)
        HypercubeProtobufSerializer.equal("a", "a")
    }

    void 'test addValue'() {
        when:
        HPS serializer
        PackedCellBuilder packer
        def addValues = { List vals ->
            (serializer, packer) = defaultPackedSerializer
            packer.valueType = vals.find().class
            vals.each { packer.addValue(new MockValue([value: it], (MockHypercube) serializer.cube)) }
        }

        addValues([1,2,3,4,5])

        then:
        packer.builder.numericValuesList == [1,2,3,4,5]
        packer.builder.stringValuesList == []
        packer.builder.nullValueIndicesList == []

        when:
        addValues(["a", "b", "c", "d"])

        then:
        packer.builder.stringValuesList == ["a", "b", "c", "d"]
        packer.builder.numericValuesList == []
        packer.builder.nullValueIndicesList == []

        when:
        addValues([null, 1, 2, null, 3, 4, null])

        then:
        packer.builder.numericValuesList == [1,2,3,4]
        packer.builder.stringValuesList == []
        packer.builder.nullValueIndicesList == [1,4,7]

        when:
        addValues(["a", null, null, null])

        then:
        packer.builder.stringValuesList == ["a"]
        packer.builder.numericValuesList == []
        packer.builder.nullValueIndicesList == [2,3,4]
    }

    void 'test putValues'() {
        when:
        HPS serializer
        PackedCellBuilder packer
        def type = { List<List> lists -> for(list in lists) { for(elem in list) { if(elem != null) return elem.class }}}
        def putValues = { List<List> vals ->
            (serializer, packer) = defaultPackedSerializer
            packer.valueType = type(vals)
            packer.putValues(vals.collectNested { new MockValue([value: it], (MockHypercube) serializer.cube) })
        }

        putValues([[], [1, 2, 3], [4, 5, 6]])

        then:
        packer.builder.stringValuesList == []
        packer.builder.numericValuesList == [1,2,3,4,5,6]
        packer.builder.nullElementCount == 0
        packer.builder.nullValueIndicesList == []
        packer.builder.sampleCountsList == [3, 3]
        packer.builder.absentValuesList == []

        when:
        putValues([['a', 'b'], ['c', 'd'], [], ['e']])

        then:
        packer.builder.stringValuesList == ['a', 'b', 'c', 'd', 'e']
        packer.builder.numericValuesList == []
        packer.builder.nullElementCount == 2
        packer.builder.nullValueIndicesList == []
        packer.builder.sampleCountsList == [2,0,1]
        packer.builder.absentValuesList == []

        when:
        putValues([[], [1], [2], [3], [4]])

        then:
        packer.builder.stringValuesList == []
        packer.builder.numericValuesList == [1,2,3,4]
        packer.builder.nullElementCount == 0
        packer.builder.nullValueIndicesList == []
        packer.builder.sampleCountsList == []
        packer.builder.absentValuesList == []

        when:
        putValues([[null], [null], [null], [null], [4]])

        then:
        packer.builder.stringValuesList == []
        packer.builder.numericValuesList == [4]
        packer.builder.nullElementCount == 1
        packer.builder.nullValueIndicesList == [1,2,3,4]
        packer.builder.sampleCountsList == []
        packer.builder.absentValuesList == []

        when:
        putValues([[null], [null], [null], [null], [4]])

        then:
        packer.builder.stringValuesList == []
        packer.builder.numericValuesList == [4]
        packer.builder.nullElementCount == 1
        packer.builder.nullValueIndicesList == [1,2,3,4]
        packer.builder.sampleCountsList == []
        packer.builder.absentValuesList == []

        when:
        putValues([[null], [null, null], [], [null], ['a']])

        then:
        packer.builder.stringValuesList == ['a']
        packer.builder.numericValuesList == []
        packer.builder.nullElementCount == 1
        packer.builder.nullValueIndicesList == [1,2,3,4]
        packer.builder.sampleCountsList == [2, 0, 1, 1]
        packer.builder.absentValuesList == []

        when:
        putValues([[], [], [], [], [], ['a']])

        then:
        packer.builder.stringValuesList == ['a']
        packer.builder.numericValuesList == []
        packer.builder.nullElementCount == 0
        packer.builder.nullValueIndicesList == []
        packer.builder.sampleCountsList == []
        packer.builder.absentValuesList == [1,2,3,4]
    }

    void 'test createPackedCell'() {
        when:
        def (HPS serializer, PackedCellBuilder packer) = defaultPackedSerializer
        def cells = iterateWhile({serializer.iterator.hasNext()}) { serializer.createPackedCell() }
        DeserializationHelper.inlineDimEncodingsSeen = 0

        then:
        cells.collectMany {
            decodePackedCell(it, serializer.packedDimension, serializer.cube.dimIndexes, inlineDims)
        } as Set == observations as Set
        DeserializationHelper.inlineDimEncodingsSeen == 7
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
            [nme, new DefaultProperty(nme, nme, type)]}

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
            return elementFields == null ? null : ImmutableMap.copyOf(elementFields)
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

        def mapValues = args.values ?: []

        //for(d in dimensions) if(d instanceof MockDimension) d.values = values

        for(dim in dimensions) {
            if(dim.density.isDense) {
                dimIndexes[dim] = mapValues.collect(new LinkedHashSet()) { it[dim.name] }.findAll { it != null } as List
            }
        }

        sorting = dimensions

        values = mapValues.collect { new MockValue(it, this) }

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

    int numElementsSeen(Dimension dim) {
        assert dim.density.isDense
        dimIndexes[dim].size()
    }

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

class MockValue implements HypercubeValue {

    MockValue(Map val, MockHypercube cube) {
        this.val = val
        this.cube = cube
    }

    private val
    private MockHypercube cube

    def getValue() {
        val.value
    }

    def getAt(Dimension dim) {
        val[dim.name]
    }

    Integer getDimElementIndex(Dimension dim) {
        assert dim.density.isDense
        int idx = cube.dimIndexes[dim].indexOf(val[dim.name])
        return idx == -1 ? null : idx
    }

    def getDimKey(Dimension dim) {
        throw new NotImplementedException()
    }

    ImmutableList<Dimension> getAvailableDimensions() {
        cube.dimensions
    }
}