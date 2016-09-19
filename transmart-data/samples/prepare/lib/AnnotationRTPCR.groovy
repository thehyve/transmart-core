/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-data.
 *
 * Transmart-data is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-data.  If not, see <http://www.gnu.org/licenses/>.
 */

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
