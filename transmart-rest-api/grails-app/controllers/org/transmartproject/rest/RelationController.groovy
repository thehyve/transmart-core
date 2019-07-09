package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.pedigree.Relation
import org.transmartproject.core.pedigree.RelationRepresentation
import org.transmartproject.core.pedigree.RelationResource
import org.transmartproject.rest.user.AuthContext

class RelationController {

    @Autowired
    RelationResource relationResource

    @Autowired
    AuthContext authContext

    static responseFormats = ['json']

    def index() {
        List<Relation> relations = relationResource.getAll(authContext.user)
        respond([relations: relations.collect { Relation relation ->
                    new RelationRepresentation(
                            leftSubjectId: relation.leftSubject.id,
                            relationTypeLabel: relation.relationType.label,
                            rightSubjectId: relation.rightSubject.id,
                            biological: relation.biological,
                            shareHousehold: relation.leftSubject.id,
                    )
                }
        ])
    }

}
