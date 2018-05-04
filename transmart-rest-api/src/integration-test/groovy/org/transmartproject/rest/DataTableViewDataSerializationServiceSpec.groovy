package org.transmartproject.rest

import com.google.common.collect.HashMultiset
import com.google.common.collect.ImmutableMap
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import grails.test.mixin.integration.Integration
import grails.test.runtime.FreshRuntime
import grails.transaction.Rollback
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.StudyObjectConstraint
import org.transmartproject.core.users.User
import org.transmartproject.db.TestData
import org.transmartproject.db.dataquery.clinical.ClinicalTestData
import org.transmartproject.db.i2b2data.ConceptDimension
import org.transmartproject.db.multidimquery.DimensionImpl
import org.transmartproject.db.multidimquery.PropertyImpl
import org.transmartproject.rest.serialization.Format
import org.transmartproject.rest.serialization.tabular.DataTableTSVSerializer
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@FreshRuntime
@Rollback
@Integration
@Slf4j
class DataTableViewDataSerializationServiceSpec extends Specification {

    TestData testData
    ClinicalTestData clinicalData
    User adminUser

    @Autowired
    DataTableViewDataSerializationService serializationService

    void setupData() {
        testData = TestData.createHypercubeDefault()
        clinicalData = testData.clinicalData
        testData.saveAll()
        adminUser = BootStrap.accessLevelTestData.users[0]
    }

    void cleanup() {
        TestData.clearAllData()
        BootStrap.setupTestData()
    }

    void testBasicSerialization() {
        setupData()

        def file = new ByteArrayOutputStream()
        def zipFile = new ZipOutputStream(file)
        Constraint constraint = new StudyObjectConstraint(study: clinicalData.longitudinalStudy)
        Map config = [
                rowDimensions: ['study', 'patient'],
                columnDimensions: ['trial visit', 'concept'],
        ]

        serializationService.writeClinical(Format.TSV, constraint, adminUser, zipFile, [tableConfig: config])
        def files = parseTSVZip(file)

        expect:
        'data' in files
        HashMultiset.create(clinicalData.longitudinalClinicalFacts*.value*.toString()) == HashMultiset.create(
                files.data[2..-1].collectMany { it[2..-1]*.toString() } )
        checkDimension(DimensionImpl.STUDY, [clinicalData.longitudinalStudy], files.study)
        checkDimension(DimensionImpl.TRIAL_VISIT, clinicalData.longitudinalClinicalFacts*.trialVisit.unique(), files."trial visit")
        checkDimension(DimensionImpl.PATIENT, clinicalData.longitudinalClinicalFacts*.patient.unique(), files.patient)
        checkDimension(DimensionImpl.CONCEPT,
                ConceptDimension.findAllByConceptCodeInList(clinicalData.longitudinalClinicalFacts*.conceptCode),
                files.concept)
    }

    void testTSVDimensionSerialization() {
        // This is actually more of a unittest, so no setupData()

        when:
        def dim = [
                getName: {'testdim'},
                getElementFields: { ImmutableMap.copyOf([
                        id: new PropertyImpl('id', 'id', Integer),
                        name: new PropertyImpl('name', 'name', String),
                        map: new PropertyImpl('map', 'map', LinkedHashMap)
                ]) },
                getKey: { it.id }
        ] as Dimension

        then:
        roundTrip(dim, [[id: 1, name: "no 1", map: null]])[1] == ['1', "no 1"]
        roundTrip(dim, [[id: 1, name: "no1", map: [:]]])[1] == ["1", "no1"]
        roundTrip(dim, [[id: 1, name: "no1", map: [foo: 'bar']]]) == [['id', 'name', 'map.foo'], ["1", "no1", "bar"]]
        roundTrip(dim, [[id: 1, name: 'no1', map: [foo: [bar: 'baz', quux: [:]], bof: 3]]]) == [
                ['id', 'name', 'map.foo.bar', 'map.bof'],
                ['1', 'no1', 'baz', '3']
        ]
        roundTrip(dim, [
                [id: 1, name: 'no1', map: [foo: 3]],
                [id: 2, name: 'no2', map: [:]],
                [id: 3, name: 'no3', map: null],
        ]) == [
                ['id', 'name', 'map.foo'],
                ['1', 'no1', '3'],
                ['2', 'no2', ''],
                ['3', 'no3', '']
        ]
        roundTrip(dim, [
                [id: 1, name: 'no1', map: null],
                [id: 2, name: 'no2', map: [foo: [bar: 42]]],
                [id: 3, name: 'no3', map: [foo: [baz: 44]]]
        ]) == [
                ['id', 'name', 'map.foo.bar', 'map.foo.baz'],
                ['1', 'no1', '', ''],
                ['2', 'no2', '42', ''],
                ['3', 'no3', '', '44']
        ]
    }

    List<List<String>> roundTrip(Dimension dim, List elements) {
        def file = new ByteArrayOutputStream()
        def zipFile = new ZipOutputStream(file)
        def serializer = new DataTableTSVSerializer(adminUser, zipFile)

        serializer.zipOutStream.putNextEntry(new ZipEntry(dim.name))
        serializer.writeDimensionElements(dim, elements)
        serializer.zipOutStream.close()

        def zip = new ZipInputStream(new ByteArrayInputStream(file.toByteArray()))
        zip.getNextEntry()
        def csvData = readCSV(zip)
        // remove header and labels
        return csvData*.getAt(1..-1)
    }

    List<List<String>> readCSV(InputStream inp) {
        def csvReader = new CSVReader(new InputStreamReader(inp), DataTableTSVSerializer.COLUMN_SEPARATOR,
                // CSVReader always treats doubled quote characters as an escaped quote when inside a quoted
                // string, the configurable escape character is an additional escape character. The writer only
                // has nested quotes to escape and nothing else, so in this case we don't want to interpret any
                // additional escape characters. (By default CSVReader interprets a backslash as escape.)
                CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER)
        def data = csvReader.readAll().collect { it as List<String> }
        data
    }

    Map<String, List<List>> parseTSVZip(ByteArrayOutputStream stream) {
        def zip = new ZipInputStream(new ByteArrayInputStream(stream.toByteArray()))

        Map result = [:]

        ZipEntry entry
        while((entry = zip.getNextEntry()) != null) {
            if(!entry.name.endsWith('.tsv') && entry.name != 'metadata.json') {
                throw new IllegalStateException("Zip file contains a non-tsv file other than 'metadata.json': $entry.name")
            }

            String name = entry.name[0..-5]
            result[name] = readCSV(zip)
        }
        result
    }

    boolean checkDimension(Dimension dim, List elements, List<List> file) {
        def headers = file[0].collect { if(it.contains(".")) it.split("\\.") else it }
        def elementsMap = file[1..-1].collect {
            def map = [:]
            [headers, it].transpose().each { key, value ->
                if(key instanceof String) {
                    map[key] = value
                } else {
                    setPath(key, map, value)
                }
            }
            map.remove('label')
            map
        }

        assert elementsMap as Set ==
                elements.collect { dim.asSerializable(it).collectEntries(valuesToString) } as Set
        return true
    }

    // This closure should apply the same transform to dimension elements as the serializer does: null and '' are the
    // same and entries in (nested) maps with empty values are removed.
    // NB: the test data does not contain dimension elements with non-empty maps, so this might break.
    def valuesToString = {key, value ->
        if (value == null) {
            return [key, '']
        } else if (value instanceof Map) {
            def transformed = value.collectEntries(valuesToString)
            return transformed ? [key, transformed] : [:]
        } else {
            return [key, value.toString()]
        }
    }

    void setPath(List<String> path, object, value) {
        if(value == null) return
        if(path.size() == 1) {
            object."${path[0]}" = value
            return
        }

        def map
        switch (object."${path[0]}") {
            case null:
                map = object."${path[0]}" = [:]
                break
            case Map:
                map = object."${path[0]}"
                break
            default:
                throw new IllegalStateException("property is not a map: $path, $object")
        }

        setPath(path[1..-1], map, value)
    }

}
