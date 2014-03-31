package org.transmartproject.rest

import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.rest.protobuf.HighDimProtos

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class HighDimResourceTests extends ResourceTestCase {

    def study = 'STUDY1'
    def concept = 'bar'

    def expectedMrnaAssays = [-402, -401]*.toLong() //groovy autoconverts to BigInteger, and we have to force Long here
    def expectedMrnaRowLabels = ["1553513_at", "1553510_s_at","1553506_at"]

    def highDimIndexUrl = "/studies/${study}/concepts/${concept}/highdim"

    void testMrna() {
        HighDimResult result = getAsHighDim(getHighDimUrl('mrna'))
        assertResult(result, expectedMrnaAssays, expectedMrnaRowLabels, [])
    }

    void testMrnaDefaultRealProjection() {
        HighDimResult result = getAsHighDim(getHighDimUrl('mrna', Projection.DEFAULT_REAL_PROJECTION))
        assertResult(result, expectedMrnaAssays, expectedMrnaRowLabels, [])
    }

    void testMrnaAllDataProjection() {
        HighDimResult result = getAsHighDim(getHighDimUrl('mrna', Projection.ALL_DATA_PROJECTION))
        List<String> expectedDataColumns = ['trialName', 'rawIntensity', 'logIntensity', 'zscore']
        assertResult(result, expectedMrnaAssays, expectedMrnaRowLabels, expectedDataColumns)
    }

    private String getHighDimUrl(String dataType) {
        "$highDimIndexUrl?dataType=${dataType}"
    }

    private String getHighDimUrl(String dataType, String projection) {
        "$highDimIndexUrl?dataType=${dataType}&projection=${projection}"
    }

    /**
     * Verified the given HighDimResult is correct in terms of rows (labels) and columns (assayIds and mapColumns).
     *
     * @param result actual result to assert
     * @param expectedAssays expected assay Ids
     * @param expectedRowLabels expected row labels
     * @param mapColumns expected map columns (or [] if projection/dataType determines a cell with single double values)
     */
    private assertResult(HighDimResult result,
                         List<Long> expectedAssays,
                         List<String> expectedRowLabels,
                         List<String> mapColumns) {

        assertThat result.header.assayList*.assayId, containsInAnyOrder(expectedAssays.toArray())

        assertThat result.header.mapColumnList, containsInAnyOrder(mapColumns.toArray())

        assertThat result.rows*.label, containsInAnyOrder(expectedRowLabels.toArray())
    }

    private HighDimResult getAsHighDim(String url) {
        InputStream is = getAsInputStream(url)
        HighDimResult result = new HighDimResult()

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

    class HighDimResult {
        HighDimProtos.HighDimHeader header
        List<HighDimProtos.Row> rows = []
    }

}
