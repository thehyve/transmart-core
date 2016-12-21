package org.transmartproject.rest.serialization

import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue

/**
 * Commonalities between the JSON serialization in {@link JsonObservationsSerializer}
 * and the Protobuf serialization in {@link ProtobufObservationsSerializer}.
 */
abstract class AbstractObservationsSerializer {

    protected Hypercube cube
    protected Map<Dimension, List<Object>> dimensionElements = [:]

    protected Long determineFooterIndex(Dimension dim, Object element) {
        if (dimensionElements[dim] == null) {
            dimensionElements[dim] = []
        }
        int index = dimensionElements[dim].indexOf(element)
        if (index == -1) {
            dimensionElements[dim].add(element)
            index = dimensionElements[dim].indexOf(element)
        }
        index.longValue()
    }

    /**
     * Begins the output message.
     * @param out the stream to write to.
     */
    abstract protected void begin(OutputStream out)

    /**
     * Ends the output message.
     * @param out the stream to write to.
     */
    abstract protected void end(OutputStream out)

    /**
     * Writes an empty message.
     * @param out the stream to write to.
     */
    abstract protected void writeEmptyMessage(OutputStream out)

    /**
     * Writes the sequence of messages representing the values passed by the
     * value iterator.
     * @param out the stream to write to.
     * @param valueIterator an iterator for the values to serialize.
     */
    abstract protected void writeCells(OutputStream out, Iterator<HypercubeValue> valueIterator)

    /**
     * Writes a header message describing the dimensions of the value messages that
     * will be written.
     * @param out the stream to write to.
     */
    abstract protected void writeHeader(OutputStream out)

    /**
     * Writes a footer message containing the indexed dimension elements referred to in the value
     * messages.
     * @param out the stream to write to.
     */
    abstract protected void writeFooter(OutputStream out)

    /**
     * Writes a message or sequence of messages serializing the data in the hybercube
     * {@link #cube}.
     * Starts with {@link #begin} and ends with {@link #end}.
     * If the cube does not contain any values, an empty message is written ({@link #writeEmptyMessage}.
     * Otherwise, first the header is written ({@link #writeHeader}, then the cells serializing
     * the values in the cube ({@link #writeCells}), then the footer containing referenced objects
     * (@link #writeFooter).
     *
     * @param out the stream to write to.
     */
    void write(Map args, Hypercube cube, OutputStream out) {
        assert args == [:]
        this.cube = cube

        begin(out)
        Iterator<HypercubeValue> iterator = cube.iterator()
        if (!iterator.hasNext()) {
            writeEmptyMessage(out)
        }
        else {
            writeHeader(out)
            writeCells(out, iterator)
            writeFooter(out)
        }
        end(out)
    }

}
