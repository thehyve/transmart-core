/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.pedigree

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.transmartproject.db.i2b2data.PatientDimension
import spock.lang.Specification

@Rollback
@Integration
class RelationSpec extends Specification {

    void 'test relation type domain object mapping'() {
        when:
        def relation = RelationType.findByLabel('MZ')
        then:
        relation.id
        relation.description == 'Monozygotic twin'
        relation.symmetrical
        relation.biological
    }

    void 'test relation domain object mapping'() {
        def subject1 = PatientDimension.get(-3000)
        def subject2 = PatientDimension.get(-3001)
        when:
        def relation = Relation.findByLeftSubjectAndRightSubject(subject1, subject2)
        then:
        relation.relationType.description == 'Spouse'
        !relation.biological
        relation.shareHousehold
    }

}
