package org.transmartproject.rest.matchers

import com.google.common.collect.Lists
import org.hamcrest.Description
import org.hamcrest.DiagnosingMatcher
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.transmartproject.core.dataquery.ColumnOrderAwareDataRow
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.rest.protobuf.HighDimProtos

class HighDimResultRowsMatcher extends DiagnosingMatcher<HighDimResult> {

    boolean multiProjection

    List<ColumnOrderAwareDataRow> expectedRows

    static HighDimResultRowsMatcher hasRowsMatchingSpecsAndDataRow(
            List<ColumnOrderAwareDataRow> expectedRows, boolean multiProjection) {
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
        for (ColumnOrderAwareDataRow expectedRow in expectedRows) {
            HighDimProtos.Row gottenRow = result.rows[i]

            if (expectedRow instanceof BioMarkerDataRow) {
                // protobuf representation of null bioMarker is empty string
                def expectedBioMarker = expectedRow.bioMarker ?: ''
                def matcher = Matchers.hasProperty('bioMarker', Matchers.is(expectedBioMarker))
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
                HighDimProtos.ColumnSpec.ColumnType propertyType = resultDataSpecs[j].type

                // for each ColumnOrderAwareDataRow, data is laid out assay first, but for
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
                                              HighDimProtos.ColumnSpec.ColumnType dataPropertyType) {
        String protoBufFieldName = dataPropertyType == HighDimProtos.ColumnSpec.ColumnType.DOUBLE ?
                'doubleValueList' : 'stringValueList'
        Matchers.hasProperty(protoBufFieldName,
                Matchers.contains(expectedValues.collect { Matchers.is it })
        )
    }

    private static List convertExpectedValues(List expectedValues,
                                              String propertyName,
                                              HighDimProtos.ColumnSpec.ColumnType propertyType,
                                              int rowNumber,
                                              Description mismatchDescription) {
        if (propertyType == HighDimProtos.ColumnSpec.ColumnType.STRING) {
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