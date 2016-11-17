package org.transmartproject.db.multidimquery

import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.db.dataquery.MockTabularResult
import org.transmartproject.db.metadata.DimensionDescription
import spock.lang.Specification

import static org.transmartproject.db.multidimquery.HddTabularResultHypercubeAdapter.*


class HddTabularResultHypercubeAdapterSpec extends Specification {

    TabularResult mockTabular
    List<Patient> patients
    List<Assay> assays
    List<String> biomarkers
    List<Double> doubleValues

    private def val(int i) {doubleValues[i]}

    void setupData() {

        patients = [-42, -43, -44].collect { inTrialId ->
            [getInTrialId: {inTrialId as String}] as Patient
        }
        assays = [[-55, -56, -57], patients].transpose().collect { code, patient ->
            [getSampleCode: {code as String}, getLabel: {code as String}, getPatient: {patient}] as AssayColumn
        }

        biomarkers = "marker1 marker2 marker3 marker4".split()
        doubleValues = (Math.PI..(Math.PI+11))

        int i = 0
        def rows = biomarkers.collect {
            new BioMockRow<Double>(
                    bioMarker: it,
                    label: it,
                    cells: [val(i++), val(i++), val(i++)],
                    columns: assays,
            )
        }

        mockTabular = new MockTabularResult(
                indicesList: assays,
                rowsList: rows
        )
    }

    static class BioMockRow<CELL> extends MockRow<AssayColumn, CELL> implements BioMarkerDataRow<CELL> {
        String bioMarker
    }

    static class MockRow<COL, CELL> implements DataRow<COL, CELL> {
        List<CELL> cells

        String label

        Iterator<CELL> iterator() { cells.iterator() }
        CELL getAt(int i) { cells[i] }

        protected List<COL> columns
        @Lazy private Map<COL, Integer> index = {
            columns.withIndex().collectEntries()
        }()

        CELL getAt(COL assay) {
            cells[index[assay]]
        }
    }


    void testDoubles() {
        setupData()

        Hypercube cube = new HddTabularResultHypercubeAdapter(mockTabular)
        List<HypercubeValue> values = cube.toList()

        expect:

        values*.value == doubleValues
        cube.dimensions as List == "biomarker assay patient".split().collect { DimensionDescription.dimensionsMap[it] }
        cube.dimensionElements(patientDim) == patients
        (0..2).each {
            cube.dimensionElement(assayDim, it) == assays[it]
        }
        (0..2).each {
            cube.dimensionElementKey(patientDim, it) == patients[it].inTrialId
            cube.dimensionElementKey(assayDim, it) == assays[it].sampleCode
        }
        cube.dimensionsPreloadable == false
        cube.dimensionsPreloaded == false
        cube.dimensionElements(biomarkerDim) == biomarkers

        // todo: test that missing dimensions fail
    }

}
