package jobs.steps.helpers

import com.google.common.base.Function
import com.google.common.collect.Iterators
import com.google.common.collect.Sets
import com.google.common.io.Closer
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.exceptions.InvalidArgumentsException

/**
 * Combines several TabularResults into one.
 * The assay lists are intercepted and the result exposes only this intersection.
 * The labels of the rows are transformed so that <conceptPath>| is prefixed
 * to them.
 */
class CompositeTabularResult implements TabularResult<AssayColumn, DataRow<AssayColumn, Number>> {

    /* concept path -> tabular result */
    Map<String, TabularResult<AssayColumn, DataRow<AssayColumn, Number>>> results

    @Lazy private List<AssayColumn> commonIndicesList = {
        List<Set<AssayColumn>> indicesSetList =
                results.values()*.indicesList.
                        collect { List<AssayColumn> assays ->
                            Sets.newHashSet assays
                        }

        Set<AssayColumn> indices = indicesSetList[0]
        indicesSetList[1..-1].each { Set<AssayColumn> current ->
            indices = Sets.intersection indices, current
        }

        if (indices.empty) {
            throw new InvalidArgumentsException(
                    "The intersection of the assays of the " +
                            "${indicesSetList.size()} result sets is empty")
        }

        (indices as List).sort { it.patientInTrialId }
    }()

    @Override
    List<AssayColumn> getIndicesList() {
        commonIndicesList
    }

    @Override
    Iterator<Number> getRows() {
        // transforms the data row iterators so that they are decorated with
        // CompositeResultDataRow and concatenates the transformed iterators
        Iterators.concat(*results.collect { String conceptPath,
                                           TabularResult<AssayColumn, DataRow<AssayColumn, Number>> tr ->

            Iterators.transform(tr.getRows(), { DataRow<AssayColumn, Number> originalRow ->
                CompositeResultDataRow ret = (tr instanceof BioMarkerDataRow) ?
                        (new CompositeResultBioMarkerDataRow()) :
                        (new CompositeResultDataRow())
                ret.delegateRow  = originalRow
                ret.assayColumns = commonIndicesList
                ret.conceptPath  = conceptPath
                ret
            } as Function)
        })
    }

    @Override
    String getColumnsDimensionLabel() {
        (results.values()*.columnsDimensionLabel as Set).join('/')
    }

    @Override
    String getRowsDimensionLabel() {
        (results.values()*.rowsDimensionLabel as Set).join('/')
    }

    @Override
    void close() throws IOException {
        Closer closer = Closer.create()
        results.values().each { TabularResult r ->
            closer.register r
        }

        closer.close()
    }

    @Override
    Iterator<Number> iterator() {
        getRows()
    }
}
