package jobs.steps

import com.google.common.collect.Lists
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.rnaseq.RnaSeqValues

class RNASeqDumpDataStep extends AbstractDumpHighDimensionalDataStep {

    int rowNumber = 1

    RNASeqDumpDataStep() {
        callPerColumn = false
    }

    @Override
    protected computeCsvRow(String subsetName,
                            String seriesName,
                            DataRow genericRow,
                            AssayColumn column /* null */,
                            Object cell /* null */) {
        RegionRow<RnaSeqValues> row = genericRow
        // +1 because the first column has no header
        def line = Lists.newArrayListWithCapacity(csvHeader.size() + 1)
        line[0] = rowNumber++ as String
        line[1] = row.chromosome as String
        line[2] = row.start as String
        line[3] = row.end as String
        line[4] = row.numberOfProbes as String
        line[5] = row.cytoband

        int j = 6

        PER_ASSAY_COLUMNS.each {k, Closure<RnaSeqValues> value ->
            assays.each { AssayColumn assay ->
                line[j++] = value(row.getAt(assay)) as String
            }
        }

        line
    }

    @Lazy List<String> csvHeader = {
        List<String> r = [
                'chromosome',
                'start',
                'end',
                'num.probes',
                'cytoband',
        ];

        PER_ASSAY_COLUMNS.keySet().each {String head ->
            assays.each { AssayColumn assay ->
                r << "${head}.${assay.patientInTrialId}".toString()
            }
        }

        r
    }()

    @Lazy def assays = {
        results.values().iterator().next().indicesList
    }()


    private static final Map PER_ASSAY_COLUMNS = [
            readcount:     { RnaSeqValues v -> v.getReadCount() },
    ]

}
