package org.transmartproject.rest

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.acgh.AcghModule
import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData
import org.transmartproject.db.dataquery.highdim.mrna.DeMrnaAnnotationCoreDb
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.rest.HighDimTestData.HighDimResult
import org.transmartproject.rest.protobuf.HighDimProtos

@TestMixin(IntegrationTestMixin)
class HighDimDataServiceTests {

    @Autowired
    HighDimDataService svc

    HighDimTestData testData
    private I2b2 concept

    @Before
    void setUp() {
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

        testData.assertMrnaRows(result, projection)
    }

    @Test
    void testMrnaAllDataProjection() {
        setUpMrna()
        String projection = Projection.ALL_DATA_PROJECTION
        HighDimResult result = getProtoBufResult('mrna', projection)

        testData.assertMrnaRows(result, projection)
    }

    @Test
    void testAcgh() {
        setUpAcgh()
        String projection = AcghModule.ACGH_VALUES_PROJECTION
        HighDimResult result = getProtoBufResult('acgh', projection)

        testData.assertAcghRows(result, projection)
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

    private void debugResults(HighDimResult result) {
        println "-- header ${result.header.rowsType}|${result.header.assayCount}|${result.header.mapColumnList}"

        result.rows.each {
            println "-- row ${it.label}|${it.bioMarker}|${it.doubleValueList}|${it.mapValueList}"
        }

        testData.mrnaData.annotations.each {
            DeMrnaAnnotationCoreDb probe = it
            def allValues = testData.mrnaData.microarrayData.findAll { it.probe == probe } collect { it.rawIntensity }
            println "-- val ${it.probeId}|${it.geneSymbol}|${allValues}"
        }
    }

    private void writeToSampleFile(String dataType, String projection, byte[] contents) {
        new File("target/sample-${dataType}-${projection}.protobuf").withOutputStream { OutputStream os ->
            os.write(contents)
        }
    }

}
