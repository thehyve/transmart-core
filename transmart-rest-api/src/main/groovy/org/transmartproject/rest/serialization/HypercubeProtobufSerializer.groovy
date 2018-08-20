/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.serialization

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.core.multidimquery.SortOrder
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue
import org.transmartproject.core.multidimquery.hypercube.Property
import org.transmartproject.rest.hypercubeProto.ObservationsProto.Error
import org.transmartproject.rest.hypercubeProto.ObservationsProto.SortOrder as ProtoSortOrder
import org.transmartproject.rest.hypercubeProto.ObservationsProto.Type as ProtoType

import javax.annotation.Nonnull

import static org.transmartproject.rest.hypercubeProto.ObservationsProto.*

@Slf4j
@CompileStatic
class HypercubeProtobufSerializer extends HypercubeSerializer {

    protected Hypercube cube
    protected Dimension packedDimension
    protected boolean packingEnabled
    protected OutputStream out

    private Iterator<HypercubeValue> iterator
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

            for(Map.Entry<Dimension, SortOrder> entry : cube.sortOrder) {
                addSort(Sort.newBuilder().with {
                    setDimensionIndex(cube.dimensions.indexOf(entry.key))
                    setField(0)
                    setSortOrder(entry.value == SortOrder.DESC ? ProtoSortOrder.DESC : ProtoSortOrder.ASC)
                    build()
                })
            }
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
        for (int i=0; i<indexedDims.size(); i++) {
            Integer idx = value.getDimElementIndex(indexedDims[i])
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

    private Value.Builder buildValue(@Nonnull value) {
        def builder = Value.newBuilder()
        builder.clear()
        Type.get(value.class).setValue(builder, value)
        builder
    }

    DimensionElement buildDimensionElement(Dimension dim, @Nonnull Object value) {
        def builder = DimensionElement.newBuilder()
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

    protected DimensionElements buildDimensionElements(Dimension dim, List dimElements, boolean setName=true) {
        def builder = DimensionElements.newBuilder()
        if(setName) builder.name = dim.name
        //builder.perSample = false //TODO: implement this

        if(dim.elementsSerializable) {
            def fieldColumnBuilder = DimensionElementFieldColumn.newBuilder()
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

        builder.build()
    }


    protected DimensionElementFieldColumn buildElementFields(Property prop, List dimElements) {
        DimensionElementFieldColumn.Builder builder = DimensionElementFieldColumn.newBuilder()

        Type type = Type.get(prop.type)

        if (type == Type.MAP) {
            return buildMapFields(prop, type, dimElements)
        }

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

    protected DimensionElementFieldColumn buildMapFields(Property prop, Type type, List dimElements) {
        // assert type == Type.MAP

        DimensionElementFieldColumn.Builder builder = DimensionElementFieldColumn.newBuilder()
        long absentCount = 0

        Map<Object, MapFieldBuilders> keyBuilders = [:]

        for(int i=0; i<dimElements.size(); i++) {
            def element = dimElements[i]

            if (element == null) {
                builder.addAbsentValueIndices(i)
                absentCount++
                continue
            }

            Map field = (Map) prop.get(element)

            for(def entry : field) {
                def builders = keyBuilders[entry.key]
                if (builders == null) {
                    builders = keyBuilders[entry.key] = new MapFieldBuilders()

                    def mapColumn = builders.mapColumn = MapColumn.newBuilder()

                    Value.Builder keyBuilder = Value.newBuilder()
                    Type.get(entry.key.class).setValue(keyBuilder, entry.key)
                    mapColumn.setKey(keyBuilder)

                    def values = builders.values = DimensionElementFieldColumn.newBuilder()

                    for(int skipped=0; skipped<i; skipped++) {
                        values.addAbsentValueIndices(skipped)
                    }
                }

                if(entry.value == null) {
                    builders.values.addAbsentValueIndices(i)
                    continue
                }
                if(builders.type == null) {
                    builders.type = Type.get(entry.value.class)
                }

                if (builders.type == Type.MAP) {
                    def valueBuilder = Value.newBuilder()
                    builders.type.setValue(valueBuilder, entry.value)
                    builders.values.addUnpackedValue(valueBuilder)
                } else {
                    builders.type.addToColumn(builders.values, entry.value)
                }
            }
        }

        for(def builders : keyBuilders.values()) {
            if (builders.type == null) continue  // values for this key are all missing or null

            MapColumn.Builder mapColumn = builders.mapColumn
            mapColumn.setValues(builders.values)

            builder.addObjectValue(mapColumn)
        }

        if (absentCount == dimElements.size()) {
            return null
        }
        builder.build()
    }

    static class MapFieldBuilders {
        MapColumn.Builder mapColumn
        DimensionElementFieldColumn.Builder values
        Type type
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
        this.packedDimension = (Dimension) args.packedDimension
        this.packingEnabled = packedDimension != null

        this.iterator = cube.iterator()
        this.inlineDims = cube.dimensions.findAll { it != packedDimension && !it.density.isDense }
        this.indexedDims = cube.dimensions.findAll { it != packedDimension && it.density.isDense }

        try {
            buildHeader().writeDelimitedTo(out)

            while(iterator.hasNext()) {
                def message = createCell(iterator.next())
                message.build().writeDelimitedTo(out)
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

