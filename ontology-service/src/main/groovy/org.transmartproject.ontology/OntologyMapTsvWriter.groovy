/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.ontology

import com.opencsv.CSVWriter

class OntologyMapTsvWriter {

    /**
     * Convert results into TSV file format.
     * @param values
     * @return
     */
    static void write(OutputStream o, Collection<OntologyMap> values) {
        o.withWriter { Writer w ->
            CSVWriter writer = new CSVWriter(w, '\t' as char)
            try {
                writer.writeNext([
                        OntologyMap.categoryCodeHeader,
                        OntologyMap.dataLabelHeader,
                        OntologyMap.ontologyCodeHeader,
                        OntologyMap.labelHeader,
                        OntologyMap.uriHeader,
                        OntologyMap.ancestorsHeader
                        ] as String[],
                        false
                        )
                values.each { OntologyMap value ->
                    if (!value.ontologyCode) {
                        // error?
                    }
                    if (!value.label) {
                        // error?
                    }
                    writer.writeNext([
                            value.categoryCode ?: '',
                            value.dataLabel ?: '',
                            value.ontologyCode,
                            value.label,
                            value.uri ?: '',
                            // Note: this only works if the ancestor codes do not contain commas.
                            value.ancestors?.collect { it.trim() }?.join(',') ?: ''
                            ] as String[],
                            false
                    )
                }
            } finally {
                writer.close()
            }
        }
    }


}
