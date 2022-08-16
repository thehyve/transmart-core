package org.transmartproject.db.pedigree

import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.pedigree.RelationRepresentation
import org.transmartproject.core.pedigree.RelationResource
import org.transmartproject.core.users.User
import org.transmartproject.db.ontology.MDStudiesService

@Transactional(readOnly = true)
@CompileStatic
class RelationService implements RelationResource {

    @Autowired
    MDStudiesService studiesService

    @Override
    List<RelationRepresentation> getAll(User user) throws AccessDeniedException {
        if (!studiesService.areAllStudiesPublic() && !user.admin) {
            throw new AccessDeniedException("Denied $user.username access to relations data.")
        }
        def relations = Relation.findAll() as List<org.transmartproject.core.pedigree.Relation>
        relationsToRelationRepresentations(relations)

    }

    private static List<RelationRepresentation> relationsToRelationRepresentations(
            List<org.transmartproject.core.pedigree.Relation> relations) {

        relations.collect { org.transmartproject.core.pedigree.Relation relation ->
            relationToRelationRepresentation(relation)
        }
    }

    private static RelationRepresentation relationToRelationRepresentation(
            org.transmartproject.core.pedigree.Relation relation) {

        new RelationRepresentation(
                leftSubjectId: relation.leftSubject.id,
                relationTypeLabel: relation.relationType.label,
                rightSubjectId: relation.rightSubject.id,
                biological: relation.biological,
                shareHousehold: relation.shareHousehold
        )
    }

}
