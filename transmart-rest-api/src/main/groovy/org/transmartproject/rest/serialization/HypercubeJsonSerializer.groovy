/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest.serialization

import com.google.gson.stream.JsonWriter
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue

import java.time.Instant

/**
 * <code>
 *                            ...,.,:,;lokxcl:;
 *                       .;ll:lOk,..    ..  .l0k:
 *                     .cd,cdo;;;c          .d:OO.
 *                     lc:::,..,.           ox;.
 *                    :l'ldc.',             xk.
 *                    o',x:.  ...           .,o:cl
 *                   ll.'l::'                    :c
 *                   x';,,;,. .'               .,;k'
 *                  'x  .'.   ,     ...  ,ll;lk0ck0K    .;::c.
 *                  ld  .   .,:   ;dx0o;ookXKkcc;,x0. .ooooxd;
 *                  l,     .lN:   :KXK00X0dc.lckOOkKlldxOc'.
 *                  oO.     cO0.  'kdcdKK,     'c.'odOd;
 *                   k,     ;Ooo .:x00Kc        ;;;Ox
 *                   .:o.   .x';;:Kxxdc.        lKk;
 *                     .x'   'o .kkOx; .'       cKl
 *                      :c.   0. l,'::  .,,. ..,'x.
 *                      :l,  .Nc;kclk;    .l,'kk.
 *                      ;l. .;O,0ON0d.       .;o
 *                      :, .'ddoXdol'       ...d.
 *                      .:.l,''ckX;d.          c:.....'c:;ccc:;:,.
 *                       oco:. ,kdc.           .,,,.....        .ol
 *                      .kkc;c0O.                                 .x.
 *                   .co:.;o0Oc,..                                  k.
 *                  .0c.,dkkd.    .''.                              :l
 *                .cc:lO:x0c         ..''                    .'.    :d
 *           cxold; oo' ;Oo.            .::;.               .llx:   ld
 *          o:.:d0oko   ,o., ,             .';,.          '.:: .k   lk
 *        'l: ..o0cdo.  .c.;;..                ,:.       ..    .x    0;
 *       dKocxdkxO,  ,   .dx..                   .,'.        . .d.   lk
 *     .kx::OXk0k,  .:     ;  '.                    ';,.    ,: ck.    X'
 *      klkKo0l    ;':.    ..                         .;,   ;' ,l     kc
 * </code>
 *
 * (original by Bertel Thorvaldsen - Image:ThorvaldsensJason.jpg, Public Domain,
 * https://commons.wikimedia.org/w/index.php?curid=4659064.)
 *
 * Serializes a {@link Hypercube} to JSON format.
 */
@CompileStatic
class HypercubeJsonSerializer extends HypercubeSerializer {

    /**
     * Contains information about a field of a dimension
     */
    static class Field {
        String name
        Type type
    }

    /**
     * Contains information about a dimension
     */
    static class DimensionProperties {
        String name
        Type type
        List<Field> fields
        boolean inline = false
    }

    /**
     * Contains information about an observation.
     */
    static class Cell {
        /**
         * The list of inlined values of this observation. This may contain nulls
         */
        List<Object> inlineDimensions = []
        /**
         * The list of indexes of indexed values of this observation. This may contain nulls.
         */
        List<Integer> dimensionIndexes = []
        /**
         * The numeric value of this observation if it is numeric.
         */
        Number numericValue
        /**
         * The string value of this observation if it is of type text.
         */
        String stringValue
    }

    protected Hypercube cube
    protected JsonWriter writer

    /**
     * Begins the output message.
     * @param out the stream to write to.
     */
    protected void begin() {
        writer.beginObject()
    }

    /**
     * Ends the output message.
     * @param out the stream to write to.
     */
    protected void end() {
        writer.endObject()
        writer.flush()
    }

    /**
     * Build an dimensional object to serialize using the field descriptions of the dimension.
     * @param dim the dimension to serialize the object for.
     * @param dimElement the value to serialize.
     * @return an object to use for writing.
     */
    protected static Object buildDimensionElement(Dimension dim, Object dimElement) {
        if (dimElement == null) return null
        if (dim.elementsSerializable) {
            return dimElement
        } else {
            def value = [:] as Map<String, Object>
            for(prop in dim.elementFields.values()) {
                value[prop.name] = prop.get(dimElement)
            }
            return value
        }
    }

    /**
     * Create a cell with either numeric or string value
     * and inlined values for the inlined dimensions and indexes for the other dimensions.
     * @param value the hypercube value.
     * @return the cell representing the serialised value.
     */
    protected Cell createCell(HypercubeValue value) {
        def cell = new Cell()
        if (value.value != null) {
            if (value.value instanceof Number) {
                cell.numericValue = value.value as Double
            } else {
                cell.stringValue = value.value as String
            }
        }
        for (Dimension dim : cube.dimensions) {
            if (!dim.density.isDense) {
                // Add the value element inline
                cell.inlineDimensions << buildDimensionElement(dim, value[dim])
            } else {
                // Add index to footer element inline. This may be null.
                cell.dimensionIndexes << value.getDimElementIndex(dim)
            }
        }
        cell
    }

    protected void writeValue(Object value) {
        if (value == null) {
            writer.nullValue()
        } else if (value instanceof String) {
            writer.value(value as String)
        } else if (value instanceof Number) {
            writer.value(value as Double)
        } else if (value instanceof Date) {
            def time = Instant.ofEpochMilli((value as Date).time).toString()
            writer.value(time)
        } else if (value instanceof Map) {
            def obj = value as Map
            writer.beginObject()
            obj.each { k, v ->
                writer.name(k as String)
                writeValue(v)
            }
            writer.endObject()
        } else {
            writer.value(value.toString())
        }
    }

    protected void writeCell(Cell cell) {
        writer.beginObject()
        writer.name('inlineDimensions')
        writer.beginArray()
        cell.inlineDimensions.each {
            writeValue(it)
        }
        writer.endArray()
        writer.name('dimensionIndexes')
        writer.beginArray()
        cell.dimensionIndexes.each {
            writer.value(it)
        }
        writer.endArray()
        if (cell.numericValue != null) {
            writer.name('numericValue').value(cell.numericValue)
        } else if (cell.stringValue) {
            writer.name('stringValue').value(cell.stringValue)
        }
        writer.endObject()
        writer.flush()
    }

    /**
     * Writes the sequence of messages representing the values passed by the
     * value iterator.
     * @param out the stream to write to.
     * @param valueIterator an iterator for the values to serialize.
     */
    protected void writeCells() {
        Iterator<HypercubeValue> it = cube.iterator()
        writer.name('cells')
        writer.beginArray()
        while (it.hasNext()) {
            def message = createCell(it.next())
            writeCell(message)
        }
        writer.endArray()
    }

    /**
     * Build the list of {@link DimensionProperties} to be serialized in the header.
     * @return a list of dimension declarations.
     */
    protected List<DimensionProperties> buildDimensionDeclarations() {
        def declarations = cube.dimensions.collect { dim ->
            // Sparse dimensions are inlined, dense dimensions are referred to by indexes
            // (referring to objects in the footer message).
            def dimensionProperties = new DimensionProperties(name: dim.name, inline: dim.density.isSparse)
            if(dim.elementsSerializable) {
                dimensionProperties.type = Type.get(dim.elementType)
            } else {
                dimensionProperties.fields = dim.elementFields.values().asList().collect {
                    new Field(name: it.name, type: Type.get(it.type))
                }
            }

            dimensionProperties
        }
        declarations
    }

    protected void writeField(Field field) {
        writer.beginObject()
        writer.name('name').value(field.name)
        writer.name('type').value(field.type.jsonType)
        writer.endObject()
    }

    protected void writeDimensionProperties(DimensionProperties dimension) {
        writer.beginObject()
        writer.name('name').value(dimension.name)
        writer.name('type').value(dimension.type?.jsonType ?: 'Object')
        if (dimension.fields) {
            writer.name('fields')
            writer.beginArray()
            dimension.fields.each {
                writeField(it)
            }
            writer.endArray()
        }
        if (dimension.inline) {
            writer.name('inline').value(true)
        }
        writer.endObject()
    }

    /**
     * Writes a header message describing the dimensions of the value messages that
     * will be written.
     * @param out the stream to write to.
     */
    protected void writeHeader() {
        writer.name('dimensionDeclarations')
        writer.beginArray()
        buildDimensionDeclarations().each {
            writeDimensionProperties(it)
        }
        writer.endArray()
    }

    /**
     * Writes a footer message containing the indexed dimension elements referred to in the value
     * messages.
     * @param out the stream to write to.
     */
    protected void writeFooter() {
        writer.name('dimensionElements')
        writer.beginObject()
        for(dim in cube.dimensions.findAll { it.density.isDense }) {
            writer.name(dim.name)
            writer.beginArray()
            cube.dimensionElements(dim).each {
                writeValue(buildDimensionElement(dim, it))
            }
            writer.endArray()
        }
        writer.endObject()
    }

    /**
     * Writes a message or sequence of messages serializing the data in the hybercube
     * {@link #cube}.
     * First the header is written ({@link #writeHeader}, then the cells serializing
     * the values in the cube ({@link #writeCells}), then the footer containing referenced objects
     * (@link #writeFooter).
     *
     * @param out the stream to write to.
     */
    void write(Map args, Hypercube cube, OutputStream out) {
        this.cube = cube
        this.writer = new JsonWriter(new PrintWriter(new BufferedOutputStream(out)))
        begin()
        writeHeader()
        writeCells()
        writeFooter()
        end()
    }
}
