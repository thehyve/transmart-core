package org.transmartproject.batch.ontologymapping

import org.springframework.batch.item.ItemWriter
import org.springframework.core.io.FileSystemResource
import org.transmartproject.batch.clinical.ontology.OntologyNode

class OntologyMappingWriter implements ItemWriter<OntologyNode> {

    private FileSystemResource resource

    OntologyMappingWriter(FileSystemResource resource) {
        this.resource = resource
    }

    @Override
    void write(List<? extends OntologyNode> items) throws Exception {
        resource.outputStream.withPrintWriter { Writer w ->
            items.each {
                w.write(it.code + '\n')
            }
        }
    }
}
