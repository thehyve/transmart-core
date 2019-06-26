package org.transmartproject.core.serialization

import com.google.gson.stream.JsonWriter
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.hypercube.Dimension

import java.time.Instant

@CompileStatic
class DimensionElementSerializer {

    protected Dimension dimension
    protected Iterable elements
    protected JsonWriter writer

    /**
     * Creates a dimension elements serializer.
     *
     * @param elements the elements to serialize.
     * @param out the stream to write to.
     */
    DimensionElementSerializer(Dimension dimension, Iterable elements, OutputStream out) {
        this.dimension = dimension
        this.elements = elements
        this.writer = new JsonWriter(new BufferedWriter(
                new OutputStreamWriter(out),
                // large 32k chars buffer to reduce overhead
                32*1024))
    }

    /**
     * Begins the output message.
     */
    protected void begin() {
        writer.beginObject()
    }

    /**
     * Ends the output message.
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

    protected void writeValue(Object value) {
        if (value == null) {
            writer.nullValue()
        } else if (value instanceof String) {
            writer.value((String) value)
        } else if (value instanceof Date) {
            def time = Instant.ofEpochMilli(((Date) value).time).toString()
            writer.value(time)
        } else if (value instanceof Number) {
            writer.value((Number) value)
        } else if (value instanceof Map) {
            Map obj = (Map) value
            writer.beginObject()
            for (Map.Entry e : obj) {
                writer.name((String) e.key)
                writeValue(e.value)
            }
            writer.endObject()
        } else {
            writer.value(value.toString())
        }
    }

    /**
     * Writes a footer message containing the indexed dimension elements referred to in the value
     * messages.
     */
    protected void writeElements() {
        writer.name('elements')
        writer.beginArray()
        for(def element: elements) {
            writeValue(buildDimensionElement(dimension, element))
        }
        writer.endArray()
    }

    /**
     * Writes a message or sequence of messages serializing the dimension elements.
     */
    void write() {
        begin()
        writeElements()
        end()
    }

}
