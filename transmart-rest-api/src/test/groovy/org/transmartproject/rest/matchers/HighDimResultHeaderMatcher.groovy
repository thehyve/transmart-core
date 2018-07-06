package org.transmartproject.rest.matchers

import org.hamcrest.Description
import org.hamcrest.DiagnosingMatcher
import org.thehyve.commons.test.FastMatchers
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.rest.protobuf.HighDimProtos

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
            DiagnosingMatcher matcher = FastMatchers.propsWith(
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
                    HighDimProtos.ColumnSpec.ColumnType.DOUBLE :
                    HighDimProtos.ColumnSpec.ColumnType.STRING
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