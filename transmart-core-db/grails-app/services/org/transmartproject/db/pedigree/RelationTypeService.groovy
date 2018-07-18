package org.transmartproject.db.pedigree

import grails.transaction.Transactional
import groovy.transform.CompileStatic
import org.hibernate.SessionFactory
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Restrictions
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.pedigree.RelationTypeResource

@Transactional(readOnly = true)
@CompileStatic
class RelationTypeService implements RelationTypeResource {

    @Autowired
    SessionFactory sessionFactory

    @Override
    List<org.transmartproject.core.pedigree.RelationType> getAll() {
        RelationType.findAll() as List<org.transmartproject.core.pedigree.RelationType>
    }

    @Override
    org.transmartproject.core.pedigree.RelationType getByLabel(String label) {
        (org.transmartproject.core.pedigree.RelationType)DetachedCriteria.forClass(RelationType)
                .add(Restrictions.eq('label', label))
                .getExecutableCriteria(sessionFactory.currentSession).uniqueResult()
    }

}
