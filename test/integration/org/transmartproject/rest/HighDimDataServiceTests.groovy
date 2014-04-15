package org.transmartproject.rest

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.InMemoryTabularResult
import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.rest.HighDimTestData.HighDimResult
import org.transmartproject.rest.protobuf.HighDimBuilder
import org.transmartproject.rest.protobuf.HighDimProtos
import org.transmartproject.rest.protobuf.HighDimProtos.Row
import org.transmartproject.rest.protobuf.HighDimProtos.ColumnSpec
import org.transmartproject.rest.protobuf.HighDimProtos.ColumnSpec.ColumnType

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.thehyve.commons.test.FastMatchers.listOf
import static org.thehyve.commons.test.FastMatchers.listOfWithOrder
import static org.thehyve.commons.test.FastMatchers.propsWith

@TestMixin(IntegrationTestMixin)
class HighDimDataServiceTests {

    @Autowired
    HighDimDataService svc

    HighDimTestData testData
    I2b2 concept

    TabularResult<AssayColumn, DataRow> collectedTable
    List<DataRow> expectedRows
    boolean checkBioMarker
    boolean isDoubleType

    @Before
    void setUp() {
        svc.resultTransformer = { TabularResult source ->
            collectedTable = new InMemoryTabularResult(source) //collecting the result
            expectedRows = collectedTable.rows.collect()
            DataRow firstRow = expectedRows[0]
            checkBioMarker = firstRow instanceof BioMarkerDataRow
            isDoubleType = (firstRow.getAt(0) instanceof Number)
            collectedTable
        }

        testData = new HighDimTestData()
        concept = testData.conceptData.addLeafConcept()
    }

    private void setUpMrna() {
        testData.mrnaData = new MrnaTestData(concept.code)
        testData.saveAll()
        testData.mrnaData.updateDoubleScaledValues()
    }

    private void setUpAcgh() {
        testData.acghData = new AcghTestData(concept.code)
        testData.saveAll()
    }

    @Test
    void testMrnaDefaultRealProjection() {
        setUpMrna()
        String projection = Projection.DEFAULT_REAL_PROJECTION
        HighDimResult result = getProtoBufResult('mrna', projection)

        assertResults(result, 'mrna', projection)
    }

    @Test
    void testMrnaAllDataProjection() {
        setUpMrna()
        String projection = Projection.ALL_DATA_PROJECTION
        HighDimResult result = getProtoBufResult('mrna', projection)

        assertResults(result, 'mrna', projection)
    }

    @Test
    void testAcgh() {
        setUpAcgh()
        String projection = 'acgh_values'
        HighDimResult result = getProtoBufResult('acgh', projection)

        assertResults(result, 'acgh', projection)
    }

    private assertResults(HighDimResult input, String dataType, String projection) {

        //asserting header Assays
        List expectedAssays = collectedTable.indicesList.collect { assayColumnMatcher(it) }
        assertThat input.header.assayList, listOfWithOrder(expectedAssays)

        Projection proj = svc.getProjection(dataType, projection)
        boolean multiValued = proj instanceof MultiValueProjection

        //asserting header ColumnSpecs
        Map<String,Class> dataProperties = HighDimBuilder.getDataProperties(proj)
        assertThat input.header.columnSpecList, columnSpecMatcher(dataProperties)

        //asserting row data
        assertRowData(input, multiValued)
    }

    private void assertRowData(HighDimResult input, boolean multiValued) {
        //asserting row count
        List<Row> actualRows = input.rows
        assertThat actualRows.size(), is (expectedRows.size())

        List<ColumnSpec> columnSpecs = input.header.columnSpecList

        //asserting row data
        actualRows.eachWithIndex{ Row actualRow, int i ->
            DataRow dataRow = expectedRows[i]
            Map rowPropMap = [
                    label: dataRow.label,
            ]

            if (checkBioMarker) {
                rowPropMap['bioMarker'] = dataRow.bioMarker
            }

            rowPropMap['valueList'] = columnValueMatcher(dataRow, columnSpecs, multiValued)

            assertThat actualRow, propsWith(rowPropMap)
        }
    }

    private Matcher columnValueMatcher(DataRow dataRow, List<ColumnSpec> columnSpecs, boolean multiValued) {
        List<List> list

        if (multiValued) {
            list = columnSpecs.collect{ [ ] }
            dataRow.iterator().each { Object cell ->
                columnSpecs.eachWithIndex{ ColumnSpec spec, int i ->
                    String col = spec.name
                    List values = list[i]

                    if (spec.type == ColumnType.DOUBLE) {
                        values.add( cell."$col" as Double)
                    } else {
                        values.add( HighDimBuilder.safeString(cell."$col"))
                    }
                }
            }

        } else {
            list = [ dataRow.iterator().collect { it as Double} ]
        }

        List<Matcher> matchers = []
        list.eachWithIndex { List columnValues, int i ->
            ColumnType type = columnSpecs[i].type
            String prop = "${type.name().toLowerCase()}ValueList" //dynamic field name by type
            matchers.add( propsWith((prop): columnValues) )
        }

        listOfWithOrder(matchers)
    }

    private Matcher columnSpecMatcher(Map<String, Class> dataProperties) {

        listOf(dataProperties.collect {
            propsWith([
                name: it.key,
                type: HighDimBuilder.typeForClass(it.value)
            ])
        })
    }

    Matcher assayColumnMatcher(AssayColumn col) {
        Map props = [
                assayId: col.id,
                patientId: col.patientInTrialId,
                sampleTypeName: col.sampleType.label,
                timepointName: col.timepoint.label,
                tissueTypeName: col.tissueType.label,
                platform: col.platform.id,
                sampleCode: col.sampleCode
        ]

        propsWith(props)
    }

    HighDimResult getProtoBufResult(String dataType, String projection) {
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        svc.write(concept.key, dataType, projection, out)
        byte[] contents = out.toByteArray()
        out.close()

        //writeToSampleFile(dataType, projection, contents)

        HighDimResult result = new HighDimResult()
        InputStream is = new ByteArrayInputStream(contents)
        try {
            result.header = HighDimProtos.HighDimHeader.parseDelimitedFrom(is)
            HighDimProtos.Row row
            while ((row = HighDimProtos.Row.parseDelimitedFrom(is)) != null) {
                result.rows << row
            }
        } finally {
            is.close()
        }

        result
    }
    private void writeToSampleFile(String dataType, String projection, byte[] contents) {
        new File("target/sample-${dataType}-${projection}.protobuf").withOutputStream { OutputStream os ->
            os.write(contents)
        }
    }

}
