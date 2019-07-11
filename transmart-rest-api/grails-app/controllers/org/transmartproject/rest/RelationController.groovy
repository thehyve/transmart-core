package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.pedigree.RelationResource
import org.transmartproject.rest.user.AuthContext

class RelationController {

    @Autowired
    RelationResource relationResource

    @Autowired
    AuthContext authContext

    static responseFormats = ['json']

    def index() {
        respond([relations: relationResource.getAll(authContext.user)])
    }

}
