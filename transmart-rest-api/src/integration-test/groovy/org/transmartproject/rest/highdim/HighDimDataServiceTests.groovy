/*
 * Copyright 2014 Janssen Research & Development, LLC.
 *
 * This file is part of REST API: transMART's plugin exposing tranSMART's
 * data via an HTTP-accessible RESTful API.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version, along with the following terms:
 *
 *   1. You may convey a work based on this program in accordance with
 *      section 5, provided that you retain the above notices.
 *   2. You may convey verbatim copies of this program code as you receive
 *      it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.rest.highdim

import com.google.common.collect.Lists
import grails.test.mixin.integration.Integration
import org.hamcrest.Description
import org.hamcrest.DiagnosingMatcher
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.InMemoryTabularResult
import org.transmartproject.db.dataquery.highdim.acgh.AcghTestData
import org.transmartproject.db.dataquery.highdim.mrna.MrnaTestData
import org.transmartproject.db.dataquery.highdim.vcf.VcfTestData
import org.transmartproject.db.ontology.I2b2
import org.transmartproject.rest.HighDimDataService
import org.transmartproject.rest.protobuf.HighDimProtos
import org.transmartproject.rest.protobuf.HighDimProtos.ColumnSpec.ColumnType
import org.transmartproject.rest.protobuf.HighDimProtos.Row

import static org.hamcrest.Matchers.*
import static org.thehyve.commons.test.FastMatchers.propsWith
import static org.transmartproject.rest.highdim.HighDimResultHeaderMatcher.hasHeaderWithAssaysAndColumns
import static org.transmartproject.rest.highdim.HighDimResultRowsMatcher.hasRowsMatchingSpecsAndDataRow

@Integration
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

    private void setUpVcf() {
        testData.vcfData = new VcfTestData(concept.code)
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

    @Test
    void testVcf_singleFieldProjection() {
        setUpVcf()
        String projection = 'variant'
        HighDimResult result = getProtoBufResult('vcf', projection)

        assertResults(result, 'vcf', projection, [value: String])
    }

    private void assertResults(HighDimResult input,
                               String dataType,
                               String projection,
                               Map dataPropertyTypes = null /* autodetect */) {

        List<AssayColumn> expectedAssays = collectedTable.indicesList

        Projection proj = svc.getProjection(dataType, projection)
        Map<String,Class> dataProperties = dataPropertyTypes
        if (!dataProperties) {
            dataProperties = proj instanceof  MultiValueProjection ?
                   proj.dataProperties :
                   [value: Double] // ASSUMPTION. Provide arg if it doesn't hold
        }

        // assert header data
        MatcherAssert.that input,
                hasHeaderWithAssaysAndColumns(expectedAssays, dataProperties)

        //asserting row data
        MatcherAssert.that input,
                hasRowsMatchingSpecsAndDataRow(
                        expectedRows,
                        proj instanceof  MultiValueProjection)
    }

    HighDimResult getProtoBufResult(String dataType, String projection) {
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        svc.write(concept.key, dataType, projection, null, null, out)
        byte[] contents = out.toByteArray()
        out.close()

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

}

class HighDimResult {
    HighDimProtos.HighDimHeader header
    List<HighDimProtos.Row> rows = []

    List<Long> getAssayIds() {
        header.assayList*.assayId //order of assays in the actual data
    }
}

class HighDimResultHeaderMatcher extends DiagnosingMatcher<HighDimResult> {

    /**
     * The expected assays in the correct order.
     */
    List<Assay> assays

    /**
     * The data properties and their value types, in no particular order.
     */
    Map<String, Class> dataProperties

    static HighDimResultHeaderMatcher hasHeaderWithAssaysAndColumns(
            List<Assay> assays, Map<String, Class> dataProperties) {
        new HighDimResultHeaderMatcher(assays: assays, dataProperties: dataProperties)
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {
        HighDimResult result = item

        def protobufAssayList = result.header.assayList
        if (result.header.assayCount != assays.size()) {
            mismatchDescription
                    .appendText('assay list size was ')
                    .appendValue(result.header.assayCount)
                    .appendText(' but we expected ')
                    .appendValue(assays.size())
            return false
        }

        for (int index = 0; index < assays.size(); index++) {
            Assay a = assays[index]
            DiagnosingMatcher matcher = propsWith(
                    assayId:        a.id,
                    patientId:      a.patientInTrialId,
                    sampleTypeName: a.sampleType.label,
                    timepointName:  a.timepoint.label,
                    tissueTypeName: a.tissueType.label,
                    platform:       a.platform.id,
                    sampleCode:     a.sampleCode)

            if (!matcher.matches(protobufAssayList[index])) {
                mismatchDescription
                        .appendText('assay ')
                        .appendValue(index)
                        .appendText(' has a mismatch: ')
                matcher.describeMismatch(item, mismatchDescription)
                return false
            }
        }

        def protobufColumns = result.header.columnSpecList
        if (dataProperties.size() != result.header.columnSpecCount) {
            mismatchDescription
                    .appendText('data property spec size was ')
                    .appendValue(result.header.columnSpecCount)
                    .appendText(' but we expected ')
                    .appendValue(dataProperties.size())
            return false
        }

        Set<String> expectedDataProperties = dataProperties.keySet()
        Set<String> gottenDataProperties = protobufColumns*.name as Set

        for (String curExpectedProperty in expectedDataProperties) {
            if (!(curExpectedProperty in gottenDataProperties)) {
                mismatchDescription
                        .appendText('data property spec with name ')
                        .appendValue(curExpectedProperty)
                        .appendText(' not found in gotten column spec names: ')
                        .appendValue(expectedDataProperties)
                return false
            }

            HighDimProtos.ColumnSpec gottenSpec =
                    protobufColumns.find { it.name == curExpectedProperty }

            def expectedDataType = Number.isAssignableFrom(
                    dataProperties[curExpectedProperty]) ?
                    ColumnType.DOUBLE :
                    ColumnType.STRING
            if (expectedDataType != gottenSpec.type) {
                mismatchDescription
                        .appendText('data property spec with name ')
                        .appendValue(curExpectedProperty)
                        .appendText(' expected to have type ')
                        .appendValue(expectedDataType)
                        .appendText(', but got ')
                        .appendValue(gottenSpec.type)
                return false
            }
        }

        true
    }

    @Override
    void describeTo(Description description) {
        description
                .appendText('protobuf high dim result with assays ')
                .appendValue(assays)
                .appendText(' and columns ')
                .appendValue(dataProperties)
    }
}

class HighDimResultRowsMatcher extends DiagnosingMatcher<HighDimResult> {

    boolean multiProjection

    List<DataRow> expectedRows

    static HighDimResultRowsMatcher hasRowsMatchingSpecsAndDataRow(
            List<DataRow> expectedRows, boolean multiProjection) {
        new HighDimResultRowsMatcher(
                expectedRows: expectedRows,
                multiProjection: multiProjection)
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {
        HighDimResult result = item

        if (expectedRows.size() != result.rows.size()) {
            mismatchDescription
                    .appendText('expected result to have size ')
                    .appendValue(expectedRows.size())
                    .appendText(' but got instead ')
                    .appendValue(result.rows.size())
            return false
        }

        int i = 0
        for (DataRow expectedRow in expectedRows) {
            Row gottenRow = result.rows[i]

            if (expectedRow instanceof BioMarkerDataRow) {
                // protobuf representation of null bioMarker is empty string
                def expectedBioMarker = expectedRow.bioMarker ?: ''
                def matcher = hasProperty('bioMarker', is(expectedBioMarker))
                if (!matcher.matches(gottenRow)) {
                    mismatchDescription
                            .appendText("on row $i expected ")
                            .appendDescriptionOf(matcher)
                            .appendText(' but: ')
                    matcher.describeMismatch(gottenRow, mismatchDescription)
                    return false
                }
            }

            List<HighDimProtos.ColumnSpec> resultDataSpecs =
                    result.header.columnSpecList
            List<?> expectedRowValues = Lists.newArrayList(expectedRow)

            for (int j = 0; j < resultDataSpecs.size(); j++) {
                String propertyName = resultDataSpecs[j].name
                ColumnType propertyType = resultDataSpecs[j].type

                // for each DataRow, data is laid out assay first, but for
                // the protobuf result it is data property ("column") first

                def expectedValues = expectedRowValues.collect { assayValue ->
                    multiProjection ?
                        assayValue?.getAt(propertyName) :
                        assayValue
                }
                HighDimProtos.ColumnValue gottenValues = gottenRow.valueList[j]

                // convert the expected values to conform to the "column spec"
                // we don't test that the column spec is actually what it's
                // supposed to be because that the job of another matcher
                // we assume it is correct
                expectedValues = convertExpectedValues(
                        expectedValues,
                        propertyName,
                        propertyType,
                        i,
                        mismatchDescription)
                if (expectedValues == null) {
                    return false
                }

                // finally match the values
                def dataPropertyAssayValuesMatcher = createValueMatcher(
                        expectedValues, propertyType)

                if (!dataPropertyAssayValuesMatcher.matches(gottenValues)) {
                    mismatchDescription
                            .appendText("row $i, data property $propertyName ")
                            .appendText('did not match. ')
                            .appendText('Expecting: ')
                            .appendDescriptionOf(dataPropertyAssayValuesMatcher)
                            .appendText(' but ')
                    dataPropertyAssayValuesMatcher.describeMismatch(
                            gottenValues, mismatchDescription)
                    return false
                }
            }

            i++
        }

        true
    }

    private static Matcher createValueMatcher(List expectedValues,
                                              ColumnType dataPropertyType) {
        String protoBufFieldName = dataPropertyType == ColumnType.DOUBLE ?
                'doubleValueList' : 'stringValueList'
        hasProperty(protoBufFieldName,
                contains(expectedValues.collect { is it })
        )
    }

    private static List convertExpectedValues(List expectedValues,
                                              String propertyName,
                                              ColumnType propertyType,
                                              int rowNumber,
                                              Description mismatchDescription) {
        if (propertyType == ColumnType.STRING) {
            expectedValues.collect { it as String }
        } else { // DOUBLE
            def result = []
            for (int k = 0; k < expectedValues.size(); k++) {
                def currentExpectedValue = expectedValues[k]
                if (currentExpectedValue != null &&
                        currentExpectedValue.getClass() == String &&
                        !currentExpectedValue.isDouble()) {
                    mismatchDescription
                            .appendText("on row $rowNumber, property $propertyName ")
                            .appendText('the values are numbers according ' +
                            'to the column spec, but got an expected value ' +
                            'that is not convertible for assay with index ')
                            .appendValue(k)
                            .appendText(', the full data row values were ')
                            .appendValue(expectedValues)
                    return null
                }

                result << (currentExpectedValue as Double)
            }

            result
        }
    }

    @Override
    void describeTo(Description description) {
        description
                .appendText('protobuf high dim result with set of rows ')
                .appendValue(expectedRows)
    }
}
