package org.transmartproject.db.pedigree

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.pedigree.RelationResource
import org.transmartproject.core.users.User
import org.transmartproject.db.ontology.MDStudiesService

@Transactional(readOnly = true)
@CompileStatic
class RelationService implements RelationResource {

    @Autowired
    MDStudiesService studiesService

    @Override
    List<org.transmartproject.core.pedigree.Relation> getAll(User user) throws AccessDeniedException {
        if (studiesService.areAllStudiesPublic() || user.admin) {
            Relation.findAll() as List<org.transmartproject.core.pedigree.Relation>
        } else {
            throw new AccessDeniedException("Denied $user.username access to relations data.")
        }
    }

}
