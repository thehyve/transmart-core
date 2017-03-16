/* Copyright © 2017 The Hyve B.V. */
package org.transmartproject.rest.serialization

import com.google.common.collect.AbstractIterator
import com.google.common.collect.ImmutableSet
import com.google.common.collect.PeekingIterator
import grails.util.Pair
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.Property
import org.transmartproject.rest.hypercubeProto.ObservationsProto.Type as ProtoType
import org.transmartproject.rest.hypercubeProto.ObservationsProto.Error

import javax.annotation.Nonnull

import static org.transmartproject.rest.hypercubeProto.ObservationsProto.*

@Slf4j
@CompileStatic
class HypercubeProtobufSerializer extends HypercubeSerializer {

    protected Hypercube cube
    protected Dimension packedDimension
    protected boolean packingEnabled
    protected OutputStream out

    private PeekingIterator<HypercubeValue> iterator
    private List<Dimension> inlineDims
    private List<Dimension> indexedDims


    protected List<DimensionDeclaration> getDimensionsDefs() {
        cube.dimensions.collect { Dimension dim ->
            DimensionDeclaration.Builder builder = DimensionDeclaration.newBuilder()

            builder.name = dim.name
            if (!dim.density.isDense) {
                // Sparse dimensions are inlined, dense dimensions are referred to by indexes
                // (referring to objects in the footer message).
                builder.inline = true
            }
            if (dim == packedDimension) {
                builder.packed = true
            }
            builder.type = dim.elementsSerializable ? Type.get(dim.elementType).protobufType : ProtoType.OBJECT

            if(!dim.elementsSerializable) {
                dim.elementFields.values().each { field ->
                    builder.addFields FieldDefinition.newBuilder().with {
                        name = field.name
                        type = Type.get(field.type).protobufType
                        assert type != ProtoType.OBJECT
                        build()
                    }
                }
            }

            builder.build()
        }
    }

    protected Header buildHeader() {
        Header.newBuilder().with {
            addAllDimensionDeclarations(dimensionsDefs)
            if(!iterator.hasNext()) last = true
            build()
        }
    }

    protected Cell.Builder createCell(HypercubeValue value) {
        def builder = Cell.newBuilder()
        if (value.value != null) {
            if (value.value instanceof Number) {
                builder.numericValue = ((Number) value.value).doubleValue()
            } else {
                builder.stringValue = value.value
            }
        }
        for (Dimension d in indexedDims) {
            Integer idx = value.getDimElementIndex(d)
            // convert to 1-based values, 0 means not present
            builder.addDimensionIndexes(idx == null ? 0 : idx+1)
        }
        for (int i=0; i<inlineDims.size(); i++) {
            Dimension dim = inlineDims[i]
            def dimElement = value[dim]
            if(dimElement != null) {
                builder.addInlineDimensions(buildDimensionElement(dim, dimElement))
            } else {
                // these values are 1-based!
                builder.addAbsentInlineDimensions(i+1)
            }
        }
        if(!iterator.hasNext()) builder.last = true
        builder
    }

    Value.Builder transferValue = Value.newBuilder()

    private Value.Builder buildValue(@Nonnull value) {
        def builder = transferValue.clear()
        builder.clear()
        Type.get(value.class).setValue(builder, value)
        builder
    }

    private DimensionElement.Builder transferDimElem = DimensionElement.newBuilder()

    DimensionElement buildDimensionElement(Dimension dim, @Nonnull Object value) {
        def builder = transferDimElem.clear()
        if (dim.elementsSerializable) {
            Value.Builder v = buildValue(dim.asSerializable(value))
            if(v == null) return null
            builder.intValue = v.intValue
            builder.doubleValue = v.doubleValue
            builder.timestampValue = v.timestampValue
            builder.stringValue = v.stringValue
        } else {
            List<Property> elementProperties = dim.elementFields.values().asList()
            for (int i=0; i<elementProperties.size(); i++) {
                def fieldVal = elementProperties[i].get(value)
                if(fieldVal == null) {
                    // 1-based!
                    builder.addAbsentFieldIndices(i+1)
                } else {
                    builder.addFields(buildValue(fieldVal).build())
                }
            }
        }
        return builder.build()
    }

    private DimensionElements.Builder debuilder = DimensionElements.newBuilder()
    protected DimensionElements.Builder buildDimensionElements(Dimension dim, List dimElements, boolean setName=true) {
        def builder = debuilder.clear()
        if(setName) builder.name = dim.name

        if(dim.elementsSerializable) {
            def fieldColumnBuilder = transferFieldColumn.clear()
            Type type = Type.get(dim.elementType)
            boolean allEmpty = true
            for(int i=0; i<dimElements.size(); i++) {
                def element = dimElements[i]
                if(element != null) {
                    allEmpty = false
                    type.addToColumn(fieldColumnBuilder, element)
                } else {
                    fieldColumnBuilder.addAbsentValueIndices(i+1)
                }
            }
            if(allEmpty) {
                builder.addAbsentFieldColumnIndices(1)
            } else {
                builder.addFields(fieldColumnBuilder.build())
            }

        } else { // dimension elements are compound
            def properties = dim.elementFields.values().asList()
            for(int i=0; i<properties.size(); i++) {
                def fieldColumn = buildElementFields(properties[i], dimElements)
                if(fieldColumn == null) {
                    builder.addAbsentFieldColumnIndices(i+1)
                } else {
                    builder.addFields(fieldColumn)
                }
            }
        }

        return builder
    }


    private DimensionElementFieldColumn.Builder transferFieldColumn = DimensionElementFieldColumn.newBuilder()

    protected DimensionElementFieldColumn buildElementFields(Property prop, List dimElements) {
        DimensionElementFieldColumn.Builder builder = transferFieldColumn.clear()

        Type type = Type.get(prop.type)

        long absentCount = 0
        for(int i=0; i<dimElements.size(); i++) {
            def elem = prop.get(dimElements[i])
            if(elem == null) {
                absentCount++
                builder.addAbsentValueIndices(i+1)
            } else {
                type.addToColumn(builder, elem)
            }
        }

        if (absentCount == dimElements.size()) {
            null
        } else {
            builder.build()
        }
    }

    PackedCellBuilder packedCellBuilder
    protected PackedCell createPackedCell() {
        packedCellBuilder.createPackedCell()
    }

    class PackedCellBuilder {

        private boolean sameIndices(HypercubeValue val, List<Integer> indices) {
            for (int i = 0; i < indices.size(); i++) {
                if (val.getDimElementIndex(indexedDims[i]) != indices[i]) return false
            }
            return true
        }

        private List<Integer> groupIndices = new ArrayList()
        private List<Integer> indices(HypercubeValue val) {
            groupIndices.clear()
            for (Dimension d in indexedDims) {
                groupIndices.add(val.getDimElementIndex(d))
            }
            groupIndices
        }

        private Class valueType
        private ArrayList<HypercubeValue> group = new ArrayList()
        private Pair<ArrayList<HypercubeValue>, Class> nextGroup() {
            group.clear()
            valueType = null
            HypercubeValue prototype = iterator.next()
            List<Integer> groupIndices = indices(prototype)
            group.add(prototype)

            while (iterator.hasNext() && sameIndices(iterator.peek(), groupIndices)) {
                Class valueType = iterator.peek().value?.class
                if (!compatibleValueType(valueType)) {
                    log.warn("Observations with incompatible value types found for the same concept or projection. " +
                            "Got $valueType.simpleName while previous observation(s) had values of type ${this.valueType.simpleName}")
                    break
                }
                group.add(iterator.next())
            }

            new Pair(group, valueType)
        }

        boolean compatibleValueType(Class type) {
            // The prototype may not have a value at all, and therefore a null value type. So we need to handle three cases:
            // null, String, or Number.
            if (valueType.is(null)) {
                valueType = type
                return true
            } else {
                return valueType == type
            }
        }

        private PackedCell.Builder builder = PackedCell.newBuilder()
        private boolean sampleCountMode
        private int elemIdx
        private int currentNumSamples
        protected PackedCell createPackedCell() {
            builder.clear()
            valueType = null
            sampleCountMode = false

            Pair<ArrayList<HypercubeValue>, Class> groupAndType = nextGroup()
            ArrayList<HypercubeValue> group = groupAndType.aValue
            valueType = groupAndType.bValue
            HypercubeValue prototype = group[0]

            putIndexedDims(prototype)

            // shuffle the group list so that values that do not have an element for the packed dimension are at the
            // front. putValues expects that
            moveNullPackedDimensionToFront(group)

            putValues(group)



            if(!iterator.hasNext()) builder.last = true

            //finalizeInlineDimensions(builder)

            builder.build()
        }

        /**
         * Moves the HypercubeValues that do not have an element for the packed dimension to the fron of the list, and
         * returns how many of them were found
         */
        private int moveNullPackedDimensionToFront(List<HypercubeValue> group) {
            int firstNonNull = 0
            for(int i=0; i<group.size(); i++) {
                if (group[i].getDimElementIndex(packedDimension) == null) {
                    group.swap(i, firstNonNull)
                    firstNonNull++
                }
            }
        }

        private void putValues(List<HypercubeValue> group) {

            // put values which do not have an element for the packed dimension. These must be at the front of the group
            // elemIdx of -1 because these values don't have an elemIdx
            elemIdx = -1
            int groupPtr = 0
            currentNumSamples = 0
            for (; groupPtr < group.size(); groupPtr++) {
                HypercubeValue hv = group[groupPtr]
                // put all the values with a null pack dimension element first
                if (hv.getDimElementIndex(packedDimension) == null) {
                    addValue(hv)
                } else {
                    break
                }
            }
            builder.setNullElementCount(currentNumSamples)
            nextElement() // We're now at element index 0

            // The list of values corresponds to the list of dimension elements. In the basic case there is a one-to-one
            // correspondence. So if we are packing along the Patient dimension, we will have a list of e.g. 50 patients
            // with indexes 0..49. If there is one observation per patient (for each concept, trial visit, and other
            // indexed dimension) the `group` list will contain a list of 50 HypercubeValue's, one for each patient for
            // the current concept-trialVisit-etc combination. In that case creating a PackedCell would be as simple as
            // taking all the 50 values an putting them in the builder. However the correspondence might not be
            // one-to-one. Observations for some patients might be missing, there could be multiple observations for a
            // patient, or there could be observations that do not belong to any patient. (The latter is unlikely for the
            // specific example of packing along the patient dimension, but in general the data model supports such cases.)
            //
            // The PackedCell supports two modes of handling these non-one-to-one cases. One uses the absentValues field
            // to indicate for which patients there is no value. This mode assumes there is at most one observation per
            // patient. The second mode uses the sampleCounts field, which for each patient indicates the number of
            // values (which might be zero). In either case the values that do not have a patient are prepended to the
            // list of values, and the nullElementCount field indicates how many such values there are. (This is expected
            // to be zero most of the time.)

            // The index into the list of dimension elements that is being packed
            assert elemIdx == 0
            // groupPtr is the index into the current group
            for (; groupPtr < group.size(); groupPtr++) {
                HypercubeValue hv = group[groupPtr]
                int idx = hv.getDimElementIndex(packedDimension) // Null values for this have already been handled

                if (idx == elemIdx) {
                    // If there is one observation per dim element, idx should be equal to elemIdx+1. This means we have
                    // found a second observation for the same dimension element. Switch to sampleCounts encoding
                    ensureSampleCountMode()
                    addValue(hv)
                    continue
                }

                // We know idx cannot be smaller than elemIdx because then the previous section would have caught that in
                // a previous iteration of this loop.
                // We are assuming that the element indexes are assigned when we iterate through this hypercube result
                // set, and therefore they are a linear sequence starting from 0 up to the number of unique patients that
                // have been seen until now.
                assert idx > elemIdx

                // We know that there cannot be any more values for the current element, so move to the next element.
                nextElement()

                while (idx > elemIdx) {
                    // No values for the current element
                    nextElement()
                }

                assert idx == elemIdx

                // We're now at the element index that matches this value
                addValue(hv)
            }
            // A final call to flush any values that haven't been added to the builder
            nextElement()
        }

        private void putIndexedDims(HypercubeValue prototype) {
            // put dimension indexes
            for (Dimension d in indexedDims) {
                Integer idx = prototype.getDimElementIndex(d)
                // convert to 1-based values, 0 means not present
                builder.addDimensionIndexes(idx == null ? 0 : idx + 1)
            }
        }

        private void addValue(HypercubeValue value) {
            currentNumSamples++

            if (valueType instanceof Number) {
                builder.addNumericValues(((Number) value.value).toDouble())
            } else {
                builder.addStringValues(value.value.toString())
            }

            addInlineDimensions(value)
        }

        private void nextElement() {
            if(sampleCountMode) {
                builder.addSampleCounts(currentNumSamples)
            } else if(currentNumSamples == 0) {
                // 1-based
                builder.addAbsentValues(elemIdx+1)
            }

            currentNumSamples = 0
            elemIdx++
        }

        private void ensureSampleCountMode() {
            if (sampleCountMode) return

            // Transform the absentValues into sampleCounts

            List<Integer> absentValueIndexes = builder.absentValuesList
            builder.clearAbsentValues()

            int j = 0
            for (int i = 0; i < elemIdx; i++) {
                if (absentValueIndexes.size() > j && absentValueIndexes[j] == i) {
                    builder.addSampleCounts(0)
                    j++
                } else {
                    builder.addSampleCounts(1)
                }
            }

            sampleCountMode = true
        }


        private List transferElements = new ArrayList()
        private DimensionElements.Builder inlineDimension(List<HypercubeValue> values, int nulls,
                                     List<Integer> sampleCounts, Dimension dim) {
            transferElements.clear()

            byte mode = getMode(values, nulls, sampleCounts, dim)

            def builder

            if(mode == PERPACK) {
                transferElements << values[0][dim]
                builder = buildDimensionElements(dim, transferElements, true)
                builder.setPerPackedCell(true)
            } else if(mode == PERPACKELEMENT) {
                if(nulls != 0) transferElements << values[0][dim]
                if(sampleCounts != null && !sampleCounts.empty) {
                    int idx = nulls
                    for(int sampleCount : sampleCounts) {
                        transferElements
                    }
                }
            }

            return builder
        }

        @TupleConstructor
        private static class SamplesIterator extends AbstractIterator<ElementSamples> implements Iterator<ElementSamples> {
            List<HypercubeValue> values
            int nulls
            List<Integer> sampleCounts
            int valuesOffset = 0
            int samplesOffset = 0

            @Override ElementSamples computeNext() {
                if(valuesOffset == 0 && nulls != 0) {
                    valuesOffset += nulls
                    return new ElementSamples(values, 0, nulls)
                }

                if(sampleCounts == null) return new ElementSamples(values, valuesOffset++, 1)
                if(samplesOffset >= sampleCounts.size()) return endOfData()

                int offset = valuesOffset
                int sampleCount = sampleCounts[samplesOffset++]
                valuesOffset += sampleCount
                return new ElementSamples(values, offset, sampleCount)
            }

            @TupleConstructor
            private static class ElementSamples extends AbstractList<HypercubeValue> {
                List<HypercubeValue> values
                int startIdx
                int size

                @Override int size() { size }
                @Override HypercubeValue get(int i) {
                    if(i > size) throw new IndexOutOfBoundsException(i.toString())
                    values[startIdx+size]
                }
            }
        }


        static final byte PERPACK = 0
        static final byte PERPACKELEMENT = 1
        static final byte PEROBSERVATION = 2

        private byte getMode(List<HypercubeValue> values, int nulls, List<Integer> sampleCounts, Dimension dim) {
            // first find out in what 'mode' this dimension is to be stored. Options: perPackedCell,
            // perPackedElement, or perSample. See the protobuf definition in observations.proto for more explanation.
            def firstElement = values[0][dim]

            // values with a null packed dimension count as a single pack-element
            for(int i=1; i<nulls; i++) {
                if(!firstElement.equals(values[i][dim])) return PEROBSERVATION
            }

            // No multiple samples, so we can just scan if any elements are different
            if(sampleCounts == null || sampleCounts.empty) {
                for(int i = nulls+1; i<values.size(); i++) {
                    if(!firstElement.equals(values[i][dim])) return PERPACKELEMENT
                }
                return PERPACK
            }

            boolean allEqual = true
            int idx = nulls
            for(int sampleCount : sampleCounts) {

                def elemForSamples = null

                for(int i=0; i<sampleCount; i++) {
                    def elem = values[idx][dim]
                    if(allEqual && firstElement.equals(elem)) continue
                    allEqual = false

                    if(elemForSamples == null) {
                        elemForSamples = elem
                        continue
                    }

                    if(!elemForSamples.equals(elem)) return PEROBSERVATION

                    idx++
                }

                elemForSamples = null
            }

            return allEqual ? PERPACK : PERPACKELEMENT
        }


        static class DimElementsBuilder {

            byte scope = PERPACK
            def lastElement
            DimensionElements.Builder builder = DimensionElements.newBuilder()
            int packElementCount = 0
            int observations = 0
            int currentPackElementObservations = 0

            protected void ensureScope(int newScope) {

            }

            protected void addElement(element) {



                currentPackElementObservations++
                if(currentPackElementObservations == 1) packElementCount++
            }

            protected void nextPackElement() {
                currentPackElementObservations = 0
            }

            protected void clear() {
                scope = PERPACK
                lastElement = null
                builder.clear()
                packElementCount = 0
                observations = 0
                currentPackElementObservations = 0
            }
        }

        private ArrayList<DimElementsBuilder> inlineDimensionBuilders = new ArrayList()
        private void addInlineDimensions(HypercubeValue value) {
            if(inlineDimensionBuilders == null) {
                inlineDimensionBuilders = new ArrayList()
                for(d in inlineDims) {
                    inlineDimensionBuilders.add(new DimElementsBuilder())
                }
            }

            for(int i=0; i<inlineDims.size(); i++) {
                Dimension d = inlineDims[i]
                DimElementsBuilder debuilder = inlineDimensionBuilders[i]

                def element = value[d]

                if(debuilder.scope == DimElementsBuilder.PERPACK) {
                    if(debuilder.lastElement == null) {
                        addInlineDim(debuilder, element)
                        continue
                    }

                    if(!debuilder.lastElement.equals(element)) {
                        debuilder.ensureScope(DimElementsBuilder.PERPACKELEMENT)
                        addInlineDim(debuilder, element)
                        continue
                    }

                }



            }

        }

        private void addInlineDim(DimElementsBuilder debuilder, element) {

        }

        private void finalizeInlineDimensions() {
            for(b in inlineDimensionBuilders) {
                if(b.scope == DimElementsBuilder.PERPACK) {
                    b.builder.setPerPackedCell(true)
                } else if(b.scope == DimElementsBuilder.PEROBSERVATION) {
                    b.builder.setPerSample(true)
                }

                builder.addInlineDimensions(b.builder)
                b.clear()
            }
        }

    }


    protected Footer buildFooter() {
        def builder = Footer.newBuilder()
        for(dim in cube.dimensions.findAll { it.density.isDense }) {
            builder.addDimension(buildDimensionElements(dim, cube.dimensionElements(dim)))
        }
        builder.build()
    }

    protected Error createError(String error) {
        Error.newBuilder().setError(error).build()
    }

    void write(Map args, Hypercube cube, OutputStream out) {
        this.cube = cube
        this.out = out

        this.inlineDims = cube.dimensions.findAll { it != packedDimension && !it.density.isDense }
        this.indexedDims = cube.dimensions.findAll { it != packedDimension && it.density.isDense }

        ImmutableSet<Dimension> sortedDims = cube.sorting.keySet()
        Dimension packDim = sortedDims.asList()[-1]
        if(args.pack && indexedDims.every { it in sortedDims } && packDim.packable.packable) {
            packedDimension = packDim
            packingEnabled = true
            packedCellBuilder = new PackedCellBuilder()
        } else {
            packedDimension = null
            packingEnabled = false
        }

        this.iterator = cube.iterator()

        try {
            buildHeader().writeDelimitedTo(out)

            if(!packingEnabled) {
                while(iterator.hasNext()) {
                    def message = createCell(iterator.next())
                    message.build().writeDelimitedTo(out)
                }
            } else {
                while(iterator.hasNext()) {
                    def message = createPackedCell()
                    message.writeDelimitedTo(out)
                }
            }

            buildFooter().writeDelimitedTo(out)

        } catch(Exception e) {

            log.error("Exception while writing protobuf result", e)

            try {
                // The Error message is compatible with Header, Footer, Cell and PackedCell so we can send that in stead
                // of any of those. This does assume that an exception is not thrown while a partial message has been
                // written. However the protobuf implementation tries its best to write full messages in one go.
                createError(e.toString()).writeDelimitedTo(out)
            } catch(Exception e2) {
                // If they're both IOException's they are probably due to the same problem. Although theoretically
                // the database i/o or some other i/o might cause the first IOException.
                if(!(e instanceof IOException && e2 instanceof IOException)) {
                    log.error("Unable to write previous error to the protobuf result stream", e2)
                }
            }

            throw e
        }
    }

}

