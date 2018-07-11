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

package org.transmartproject.rest

import org.transmartproject.core.dataquery.ColumnOrderAwareDataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.db.dataquery.InMemoryTabularResult
import org.transmartproject.db.dataquery.MockTabularResult
import org.transmartproject.db.dataquery.highdim.HighDimensionResourceService
import org.transmartproject.rest.matchers.HighDimResult
import org.transmartproject.rest.protobuf.HighDimProtos
import org.transmartproject.rest.protobug.HighDimBuilderSpec
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import static org.transmartproject.rest.matchers.HighDimResultHeaderMatcher.hasHeaderWithAssaysAndColumns
import static org.transmartproject.rest.matchers.HighDimResultRowsMatcher.hasRowsMatchingSpecsAndDataRow
import static spock.util.matcher.HamcrestSupport.that

@Ignore // FIXME: failing because of missing test data
class HighDimDataServiceSpec extends Specification {

    HighDimDataService highDimDataService
    MockTabularResult<AssayColumn, BioMarkerDataRow> mockTabularResult
    HighDimensionDataTypeResource mockHighDimensionDataTypeResource
    HighDimensionResourceService mockHighDimensionResourceService

    TabularResult<AssayColumn, ColumnOrderAwareDataRow> collectedTable
    List<ColumnOrderAwareDataRow> expectedRows
    boolean checkBioMarker
    boolean isDoubleType

    void setupData() {
        mockTabularResult = new MockTabularResult<>()

        mockHighDimensionDataTypeResource = Mock(HighDimensionDataTypeResource)
        mockHighDimensionDataTypeResource.retrieveData(_, _, _) >> {
            List<AssayConstraint> assayConstraints,
            List<DataConstraint> dataConstraints,
            Projection projection ->
                mockTabularResult
        }
        mockHighDimensionResourceService = Mock(HighDimensionResourceService)
        mockHighDimensionResourceService.getSubResourceForType(_) >> { String type ->
                    mockHighDimensionDataTypeResource
                }

        highDimDataService = new HighDimDataService()
        highDimDataService.highDimensionResourceService = mockHighDimensionResourceService

        highDimDataService.resultTransformer = { TabularResult source ->
            collectedTable = new InMemoryTabularResult(source) //collecting the result
            expectedRows = collectedTable.rows.collect()
            ColumnOrderAwareDataRow firstRow = expectedRows[0]
            checkBioMarker = firstRow instanceof BioMarkerDataRow
            isDoubleType = (firstRow.getAt(0) instanceof Number)
            collectedTable
        }

    }

    private void mockMrnaData() {
        mockTabularResult.rowsList = [new HighDimBuilderSpec.TestDataRow()]
    }

    private void mockAcghData() {
        mockTabularResult.rowsList = [new HighDimBuilderSpec.TestDataRow()]
    }

    private void mockVcfData() {
        mockTabularResult.rowsList = [new HighDimBuilderSpec.TestDataRow()]
    }

    private void mockTestData(dataType) {
        switch (dataType) {
            case 'mrna':
                mockMrnaData()
                break
            case 'acgh':
                mockAcghData()
                break
            case 'vcf':
                mockVcfData()
                break
        }
    }

    @Unroll
    void testAll() {
        given:
        setupData()

        when:
        mockTestData(dataType)
        HighDimResult result = getProtoBufResult(dataType, projection)

        Projection proj = highDimDataService.getProjection(dataType, projection)
        if (!dataProperties) {
            dataProperties = proj instanceof  MultiValueProjection ?
                    proj.dataProperties :
                    [value: Double] // ASSUMPTION. Provide arg if it doesn't hold
        }

        then:
        // assert header data
        that result,
                hasHeaderWithAssaysAndColumns(collectedTable.indicesList, dataProperties)

        //asserting row data
        that result,
                hasRowsMatchingSpecsAndDataRow(
                        expectedRows,
                        proj instanceof  MultiValueProjection)

        where:
        dataType    || projection                           || dataProperties
        'mrna'      || Projection.DEFAULT_REAL_PROJECTION   || null
        'mrna'      || Projection.ALL_DATA_PROJECTION       || null
        'acgh'      || 'acgh_values'                        || null
        'vcf'       || 'variant'                            || [value: String]
    }

    HighDimResult getProtoBufResult(String dataType, String projection) {
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        highDimDataService.write('conceptKey', dataType, projection, null, null, out)
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
