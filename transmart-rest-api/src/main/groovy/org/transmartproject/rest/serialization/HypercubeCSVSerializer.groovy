package org.transmartproject.rest.serialization

import com.opencsv.CSVWriter
import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.hypercube.Dimension
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.HypercubeValue

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@CompileStatic
class HypercubeCSVSerializer extends HypercubeSerializer {

    final static char COLUMN_SEPARATOR = '\t' as char
    final static String FORMAT_EXTENSION = ".tsv"
    protected Hypercube cube
    protected String dataType
    protected ZipOutputStream zipOutStream

    /**
     * Creates a new entry for zipOutStream
     * @param fileName
     * @param header
     * @param additionalDim
     */
    protected void createNewFile(String fileName, List header, Dimension additionalDim = null) {
        zipOutStream.putNextEntry(new ZipEntry(fileName))
        CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(zipOutStream), COLUMN_SEPARATOR)
        csvWriter.writeNext(header as String[])
        if (additionalDim == null) {
            writeObservationsColumns(csvWriter)
        } else {
            writeDimensionColumns(csvWriter, additionalDim)
        }
        csvWriter.flush()
        zipOutStream.closeEntry()
    }

    /**
     * Creates the main file for all observations
     *
     */
    protected void createObservationsFile() {
        List header = createObservationsHeader()
        String fileName = createTableName('observations')
        createNewFile(fileName, header)
    }

    /**
     * Creates a new file for each dense dimension
     *
     * TODO Merge all tables into one after implementing preloading of dimensions in the hypercube
     */
    protected void createDimensionsFiles() {
        for (dim in cube.dimensions.findAll { it.density.isDense }) {
            List header = createDimensionHeader(dim)
            String fileName = createTableName(dim.name)
            createNewFile(fileName, header, dim)
        }
    }

    /**
     * Creates a header for the main file
     *
     */
    protected List createObservationsHeader() {
        def header = []
        header << 'value'
        for (Dimension dim : cube.dimensions) {
            header << dim.name //TODO change after adding preloading of dimensions
        }
        header
    }

    /**
     * Creates a header for a dimension file
     * @param dim
     */
    protected static List createDimensionHeader(Dimension dim) {
        def header = []
        header << 'dimensionId'
        dim.elementFields ? dim.elementFields.each { header << it.value.name }
                : header << dim.name
        header
    }

    /**
     * Writes rows to the main file
     * @param writer
     */
    protected void writeObservationsColumns(CSVWriter writer) {
        Iterator<HypercubeValue> it = cube.iterator()
        while (it.hasNext()) {
            HypercubeValue value = it.next()
            def row = []
            row << value.value.toString()
            for (Dimension dim : cube.dimensions) {
                if (dim.density.isSparse) {
                    // Add the value element inline
                    row << value[dim] ?: "null"
                } else {
                    // Add index to footer element inline. This may be null.
                    row << value.getDimElementIndex(dim) ?: "null"
                }
            }
            writer.writeNext(row as String[])
        }
    }

    /**
     * Writes rows to a dimension file
     * @param csvWriter
     * @param dim
     */
    protected void writeDimensionColumns(CSVWriter csvWriter, Dimension dim) {
        cube.dimensionElements(dim).eachWithIndex { element, index ->
            def row = []
            row << index
            if (dim.elementsSerializable) {
                row.add(element)
            } else {
                row.addAll(dim.elementFields.collect { it.value.get(element).toString() })
            }
            csvWriter.writeNext(row as String[])
        }
    }

    /**
     * Creates name for a new file
     * e.g "clinical_observations.tsv"
     * @param dimension
     * @return
     */
    private String createTableName(String dimension) {
        dataType + "_" + dimension.replace(' ', '_') + FORMAT_EXTENSION
    }

    /**
     * Writes a files to zip output stream serializing the data in the hybercube
     * {@link #cube}.
     * First the main file is written ({@link #createObservationsFile}, then the files
     * for each dimension ({@link #createDimensionsFiles}).
     *
     * @param out the stream to write to.
     */
    void write(Map args, Hypercube cube, OutputStream zipOutStream) {
        this.dataType = args.dataType
        assert this.dataType != null
        this.cube = cube
        this.zipOutStream = zipOutStream as ZipOutputStream

        createObservationsFile()
        createDimensionsFiles()
    }

}
