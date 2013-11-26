package lib

import groovy.transform.ToString
import lib.soft.SoftTable

@ToString
class AnnotationRTPCR implements AutoCloseable {

    private static final String PARAMS_FILE_NAME = 'annotation_rtpcr.params'
    private static final String DATA_DIR = 'annotation_rtpcr'

    String platformId,
           title,
           organism

    SoftTable table

    private File annotationFile

    void writeFiles() {
        File studyDir = new File(platformId)
        File dataDir = new File(studyDir, DATA_DIR)
        annotationFile = new File(dataDir, "${platformId}_simplified.tsv")

        if (!studyDir.exists()) {
            studyDir.mkdir()
        }

        ParamsFile paramsFile = getParamsFile(new File(studyDir, PARAMS_FILE_NAME))
        paramsFile.write()

        if (!dataDir.exists()) {
            dataDir.mkdir()
        }

        writeAnnotationsFile()
    }

    void writeAnnotationsFile() {
        annotationFile.withWriter { BufferedWriter r ->
            r.writeLine "ID_REF\tMIRNA_ID\tSN_ID\tPLT_NAME\tORGANISM"

            Iterator rows = table.rows
            rows.next() //skip header line
            rows.each { row ->
                def values = [
                        row['ID'],
                        row['miRNA_ID'],
                        '', //SN_IS is empty... whatever this is
                        platformId,
                        organism,
                ]
                r.writeLine values.join('\t')
            }
        }
    }

    ParamsFile getParamsFile(File targetFile) {
        new ParamsFile(
                file: targetFile,
                params: [
                        PLATFORM:         platformId,
                        TITLE:            title,
                        ORGANISM:         organism,
                        ANNOTATIONS_FILE: annotationFile.name
                ]
        )
    }

    @Override
    void close() throws Exception {
        table?.close()
    }
}
