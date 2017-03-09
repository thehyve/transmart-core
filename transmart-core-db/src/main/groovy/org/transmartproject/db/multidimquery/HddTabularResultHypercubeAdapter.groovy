/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.db.multidimquery

import com.google.common.collect.AbstractIterator
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.PeekingIterator
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.TailRecursive
import org.transmartproject.core.dataquery.highdim.projections.MultiValueProjection
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.BioMarkerDataRow
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.dimensions.BioMarker
import org.transmartproject.core.multidimquery.dimensions.Order
import org.transmartproject.db.util.AbstractOneTimeCallIterable
import org.transmartproject.db.util.IndexedArraySet

@CompileStatic
class HddTabularResultHypercubeAdapter extends AbstractOneTimeCallIterable<HypercubeValue> implements Hypercube {
    private static Object typeError(cell) {
        throw new RuntimeException("HDD value '$cell' is not a Double and is not a Map, this projection is not" +
                " implemented in HddTabularResultHypercubeAdapter")
    }

    static Dimension biomarkerDim = DimensionImpl.BIOMARKER
    static Dimension assayDim = DimensionImpl.ASSAY
    static Dimension patientDim = DimensionImpl.PATIENT
    static Dimension projectionDim = DimensionImpl.PROJECTION

    private TabularResult<AssayColumn, ? extends DataRow<AssayColumn, ? /* depends on projection */>> table
    private TabularResultAdapterIterator iterator

    Projection projection

    ImmutableMap<Dimension, Order> sorting = ImmutableMap.of(biomarkerDim, Order.ASC, projectionDim, Order.ASC, assayDim, Order.ASC)
    /**
     * Either an IndexedArraySet or an ImmutableList
     * projectionFields is collected dynamically, but as all tabular cells should contain the same projection fields
     * reading just the first tabular result cell is sufficient. If the HDD tabular result
     */
    private List<String> _projectionFields = null
    List<String> getProjectionFields() {
        if(_projectionFields != null) return _projectionFields
        getIterator().hasNext() // sets _projectionFields as a side effect
        _projectionFields
    }

    @Lazy ImmutableList<Dimension> dimensions = (
        !projectionFields
            ? ImmutableList.of(biomarkerDim, assayDim, patientDim)
            : ImmutableList.of(biomarkerDim, assayDim, patientDim, projectionDim)
    )

    protected ImmutableList<Assay> assays
    protected List<Patient> patients  // replaced by an ImmutableList once we have finished iterating
    protected List<BioMarker> biomarkers = [] // idem

    HddTabularResultHypercubeAdapter(TabularResult<AssayColumn, ? extends DataRow<AssayColumn, ?>> tabularResult,
                                     Projection projection) {
        table = tabularResult
        this.projection = projection
        assays = (ImmutableList) ImmutableList.copyOf(table.getIndicesList())

        // The getAll fetches all patients in a single query. Unfortunately before that hibernate decides that it
        // needs to flush its cache and in the process of it loads the patients one by one. I have no idea why it
        // deems that necessary. The cached objects it is flushing are the assays we have here and it is cascading to
        // their patients.
        // Also using getAll requires integration tests to test this class, now we can do with only unit tests.
        // patients = new IndexedArraySet<>((List) I2b2Patient.getAll((List) assays*.patient.id))
        patients = new IndexedArraySet<>((List) assays*.patient)
    }

    protected List<? extends Object> _dimensionElems(Dimension dim) {
        if(dim == assayDim) return assays
        else if(dim == patientDim) return patients
        else if(dim == biomarkerDim) return biomarkers
        else if(dim == projectionDim && projectionFields) {
            return ImmutableList.copyOf(projectionFields)
        } else throw new InvalidArgumentsException("$dim is not present in this hypercube or is inlined")
    }

    ImmutableList<? extends Object> dimensionElements(Dimension dim) {
        // ImmutableList.copyOf is smart about not making copies of immutable lists
        ImmutableList.copyOf(_dimensionElems(dim))
    }

    Object dimensionElement(Dimension dim, Integer idx) {
        _dimensionElems(dim)[idx]
    }

    Object dimensionElementKey(Dimension dim, Integer idx) {
        def elem = dimensionElement(dim, idx)
        if(elem instanceof String) return elem
        else if(elem instanceof DataColumn) return ((DataColumn) elem).label
        else if(elem instanceof Patient) return ((Patient) elem).id
        else throw new RuntimeException("unexpected element type ${elem.class}. Expected a String, Patient, or Assay")
    }


    @Override PeekingIterator<HypercubeValue> iterator() { (PeekingIterator) super.iterator() }
    @Override PeekingIterator<HypercubeValue> getIterator() {
        iterator == null ? (iterator = new TabularResultAdapterIterator()) : iterator
    }

    class TabularResultAdapterIterator extends AbstractIterator<HypercubeValue> implements
            PeekingIterator<HypercubeValue> {
        private Iterator<? extends DataRow<AssayColumn, ?>> tabularIter = table.getRows()
        private Set<String> projectionFields = (projection instanceof MultiValueProjection) ?
                ((MultiValueProjection) projection).dataProperties.keySet() : null
        private List<HypercubeValue> nextVals = []
        private Iterator<HypercubeValue> nextIter = nextVals.iterator()

        @TailRecursive
        HypercubeValue computeNext() {
            if(nextIter.hasNext()) {
                return nextIter.next()
            }
            nextVals.clear()
            nextIter = null

            // Initialize _projectionFields
            if(_projectionFields == null) _projectionFields = new IndexedArraySet<String>()

            if(!tabularIter.hasNext()) {
                _projectionFields = ImmutableList.copyOf(_projectionFields)
                biomarkers = ImmutableList.copyOf(biomarkers)
                patients = ImmutableList.copyOf(patients)
                return endOfData()
            }

            DataRow<AssayColumn, ?> row = tabularIter.next()
            BioMarker bm = new BioMarkerAdapter(row.label,
                    row instanceof BioMarkerDataRow ? ((BioMarkerDataRow) row).bioMarker : null)
            int biomarkerIdx = biomarkers.size()
            biomarkers << bm

            if(projectionFields == null) {

                // assays.size() compiles to GroovyDefaultMethods.size(Iterable) on Groovy 2.4.7 :(
                for(int i = 0; i < ((List)assays).size(); i++) {
                    Assay assay = assays[i]
                    int patientIndex = patients.indexOf(assay.patient)
                    def value = row[i]

                    if (value == null) {
                        continue
                    } else if (value instanceof Number || value instanceof String) {
                        nextVals.add(new TabularResultAdapterValue(
                                // The type checker doesn't like a plain 'dimensions', no idea why
                                availableDimensions: getDimensions(),
                                value: value,
                                biomarker: bm,
                                assay: assay,
                                biomarkerIndex: biomarkerIdx,
                                assayIndex: i,
                                patientIndex: patientIndex,
                                projectionIndex: -1
                        ))
                    } else typeError(value)
                }

            } else {
                for(String field in projectionFields) {
                    _projectionFields.add(field)
                    int projectionIndex = _projectionFields.indexOf(field)

                    // assays.size() compiles to GroovyDefaultMethods.size(Iterable) on Groovy 2.4.7 :(
                    for(int i = 0; i < ((List)assays).size(); i++) {
                        def value = row[i].getAt(field)
                        if(value == null) continue

                        Assay assay = assays[i]
                        int patientIndex = patients.indexOf(assay.patient)

                        nextVals.add(new TabularResultAdapterValue(
                                availableDimensions: getDimensions(),
                                value: value,
                                biomarker: bm,
                                assay: assay,
                                projectionKey: field,
                                biomarkerIndex: biomarkerIdx,
                                assayIndex: i,
                                patientIndex: patientIndex,
                                projectionIndex: projectionIndex
                        ))
                    }
                }
            }

            nextIter = nextVals.iterator()
            return computeNext()
        }
    }

    static class TabularResultAdapterValue implements HypercubeValue {
        static int dimError(dim) {
            throw new IndexOutOfBoundsException("Dimension $dim is not applicable to this hypercube result")
        }

        ImmutableList<Dimension> availableDimensions
        def value
        protected BioMarker biomarker
        protected Assay assay
        protected String projectionKey
        protected int biomarkerIndex
        protected int assayIndex
        protected int patientIndex
        protected int projectionIndex

        Patient getPatient() { assay.patient }

        def getAt(Dimension dim) {
            if(dim.is(biomarkerDim)) return biomarker
            if(dim.is(assayDim)) return assay
            if(dim.is(patientDim)) return patient
            if(dim.is(projectionDim) && projectionKey != null) return projectionKey
            dimError(dim)
        }

        Integer getDimElementIndex(Dimension dim) {
            if(dim.is(biomarkerDim)) return biomarkerIndex
            if(dim.is(assayDim)) return assayIndex
            if(dim.is(patientDim)) return patientIndex
            if(dim.is(projectionDim) && projectionKey != null) return projectionIndex
            dimError(dim)
        }

        def getDimKey(Dimension dim) {
            if(dim.is(biomarkerDim)) return biomarker.biomarker ?: biomarker.label
            if(dim.is(assayDim)) return assay.sampleCode
            if(dim.is(patientDim)) return patient.id
            if(dim.is(projectionDim) && projectionKey != null) return projectionKey
            dimError(dim)
        }
    }

    @Immutable
    static class BioMarkerAdapter implements BioMarker {
        final String label
        final String biomarker
    }


    void loadDimensions() { /*no-op*/ }
    void preloadDimensions() { throw new UnsupportedOperationException() }
    final boolean dimensionsPreloadable = false
    final boolean dimensionsPreloaded = false
    boolean autoloadDimensions = true

    @Override
    void close() {
        table.close()
    }
}
