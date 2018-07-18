package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.pedigree.RelationTypeResource

class RelationTypeController {

    @Autowired
    RelationTypeResource relationTypeResource

    static responseFormats = ['json']

    def index() {
        respond([relationTypes: relationTypeResource.all])
    }

}
