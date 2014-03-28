package org.transmartproject.rest

import org.hamcrest.Matcher
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.highdim.mrna.DeMrnaAnnotationCoreDb
import org.transmartproject.db.dataquery.highdim.mrna.DeSubjectMicroarrayDataCoreDb
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.ontology.ConceptTestData
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimHeader
import org.transmartproject.rest.protobuf.HighDimProtos.HighDimHeader.RowType
import org.transmartproject.rest.protobuf.HighDimProtos.Row

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class HighDimTestData {

    //This will be very easily outdated, if any of the default property for projection changes for any data type
    static Map projectionPropertiesMap = {
        Map map  = [:]
        map.put(['mrna', Projection.DEFAULT_REAL_PROJECTION], ['rawIntensity'])
        map
    }()

    ConceptTestData conceptData = new ConceptTestData()
    MrnaTestData mrnaData

    void saveAll() {
        conceptData.saveAll()
        mrnaData?.saveAll()
    }

    void assertMrnaRows(HighDimResult input, String projection) {

        RowType rowType = input.header.rowsType

        List<Long> assayIds = input.header.assayList*.assayId //order of assays in the actual data
        List<String> valueProperties =
                (rowType == RowType.GENERAL) ?
                        input.header.mapColumnList :
                        projectionPropertiesMap.get(['mrna',projection])

        List<Matcher> matchers = mrnaData.annotations.collect {
            DeMrnaAnnotationCoreDb probe = it
            List expectedValueList = assayIds.collect { getMrnaCell(probe, it) }
            Matcher annotationMatcher = createMrnaRowMatcher(probe)
            Matcher cellMatcher = createRowValuesMatcher(rowType, valueProperties, expectedValueList)
            allOf(annotationMatcher, cellMatcher)
        }

        assertThat input.rows, containsInAnyOrder(matchers)
    }

    private DeSubjectMicroarrayDataCoreDb getMrnaCell(DeMrnaAnnotationCoreDb probe, long assayId) {
        mrnaData.microarrayData.find { probe == it.probe && assayId == it.assayId}
    }

    private Matcher createMrnaRowMatcher(DeMrnaAnnotationCoreDb probe) {
        allOf(
                hasProperty('label', is(probe.probeId)),
                hasProperty('bioMarker', is(probe.geneSymbol)),
        )
    }

    /**
     * Creates a matcher for protobuf Row cell values
     * @param rowType the type of Row in the protobuf
     * @param cellProperties the properties we need to get from each highdim cell
     * @param expectedCellList the cells we expect to get on the Row
     * @return
     */
    private Matcher createRowValuesMatcher(RowType rowType, List cellProperties, List expectedCellList) {
        switch (rowType) {
            case RowType.DOUBLE:
                return createDoubleRowValuesMatcher(cellProperties[0], expectedCellList)
            case RowType.GENERAL:
                return createMapRowValuesMatcher(cellProperties, expectedCellList)
            default:
                throw new UnsupportedOperationException()
        }
    }

    /**
     * Creates a value matcher for a complete protobuf Row (with doubles),
     * based on the expected cell values of one annotation.
     *
     * @param cellProperty the property we need to get from each highdim cell
     * @param cellList the cells we expect to get on the Row
     * @return
     */
    private Matcher createDoubleRowValuesMatcher(String cellProperty, List expectedCellList) {
        //we must enforce a Double, as hamcrest is() doesn't convert automagically BigDecimals to Doubles
        List<Double> expectedValues = expectedCellList.collect { it."$cellProperty".toDouble() }
        hasProperty('doubleValueList', is(expectedValues))
    }

    /**
     * Creates a value matcher for a complete protobuf Row (with MapValues),
     * based on the expected cell values of one annotation.
     *
     * @param cellProperties the properties we need to get from each highdim cell
     * @param expectedCellList the cells we expect to get on the Row
     * @return
     */
    private Matcher createMapRowValuesMatcher(List<String> cellProperties, List expectedCellList) {
        List<Matcher> mapValueMatchers = expectedCellList.collect { createMapValueMatcher(cellProperties, it) }
        hasProperty("mapValueList", contains(mapValueMatchers))
    }

    /**
     * Creates a value matcher for a protobuf MapValue object, based on the expected cell
     * @param cellProperties properties we need from the cell
     * @param cell expected cell
     * @return
     */
    private Matcher createMapValueMatcher(List<String> cellProperties, Object cell) {
        List<String> expectedValues = cellProperties.collect { String.valueOf(cell."$it") }
        hasProperty("valueList", is(expectedValues))
    }

    static class HighDimResult {
        HighDimHeader header
        List<Row> rows = []
    }

}
