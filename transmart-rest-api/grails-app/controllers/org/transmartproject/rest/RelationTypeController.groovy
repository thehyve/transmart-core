package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.db.pedigree.RelationType

class RelationTypeController {

    @Autowired
    VersionController versionController

    static responseFormats = ['json']

    def index() {
        respond([relationTypes: RelationType.list()])
    }

}
