/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.pedigree

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.db.i2b2data.PatientDimension
import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.user.User
import spock.lang.Specification

import static org.transmartproject.db.multidimquery.DimensionImpl.getPATIENT

@Rollback
@Integration
class RelationSpec extends Specification {

    @Autowired
    MultiDimensionalDataResource multiDimService

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
        def subject1 = PatientDimension.get(-3001)
        def subject2 = PatientDimension.get(-3002)
        when:
        def relation = Relation.findByLeftSubjectAndRightSubject(subject1, subject2)
        then:
        relation.relationType.description == 'Spouse'
        !relation.biological
        relation.shareHousehold
    }

    void "get clinical data applying relation constraint"() {
        def user = User.findByUsername('test-public-user-1')

        Constraint constraint = new RelationConstraint(
                relationTypeLabel: 'SPO'
        )

        when:
        Hypercube hypercube = multiDimService.retrieveClinicalData(constraint, user)

        then:
        def subjects = hypercube.dimensionElements(PATIENT)
        subjects*.id as Set == [-3001L, -3002L, -3004L, -3005L, -3010L, -3011L] as Set
    }

    void "get patients applying relation constraint"() {
        def user = User.findByUsername('test-public-user-1')

        when: 'no relation type specified'
        multiDimService.getDimensionElements(PATIENT, new RelationConstraint(), user)
        then: 'exception is thrown'
        def e1 = thrown(QueryBuilderException)
        e1.message == 'No null relation type found.'

        when: 'not-existed relation type specified'
        multiDimService.getDimensionElements(PATIENT,
                new RelationConstraint(
                        relationTypeLabel: 'NON-EXISTENT'
                ), user)
        then: 'exception is thrown'
        def e2 = thrown(QueryBuilderException)
        e2.message == 'No NON-EXISTENT relation type found.'

        when: 'get parents for the subject'
        def allParents = multiDimService.getDimensionElements(PATIENT,
                new RelationConstraint(
                        relationTypeLabel: 'PAR'
                ), user)
        then: 'all parents selected'
        allParents*.id as Set == [-3001L, -3002L, -3003L, -3004L, -3005L, -3010L, -3011L] as Set

        when: 'get parents for the subject'
        def parentsForSubject = multiDimService.getDimensionElements(PATIENT,
                new RelationConstraint(
                        relatedSubjectsConstraint: new PatientSetConstraint(
                                patientIds: [-3014]
                        ),
                        relationTypeLabel: 'PAR'
                ), user)
        then: 'both parents selected'
        parentsForSubject*.id as Set == [-3010L, -3011L] as Set

        when: 'get all step parents'
        def stepParents = multiDimService.getDimensionElements(PATIENT,
                new RelationConstraint(
                        relationTypeLabel: 'PAR',
                        biological: false
                ), user)
        then: 'both step parents selected'
        stepParents*.id as Set == [-3004L, -3005L] as Set

        when: 'get all parents that don\'t live with theirs kids at the same address'
        def parentsThatLiveSeparateFromTheirKids = multiDimService.getDimensionElements(PATIENT,
                new RelationConstraint(
                        relationTypeLabel: 'PAR',
                        shareHousehold: false
                ), user)
        then: 'all such parents selected'
        parentsThatLiveSeparateFromTheirKids*.id as Set == [-3002L, -3003L, -3004, -3005] as Set

        when: 'get all biological siblings that have twin kids.'
        def siblingsWitTwins = multiDimService.getDimensionElements(PATIENT,
                new RelationConstraint(
                        relatedSubjectsConstraint: new AndConstraint(args: [
                                new ConceptConstraint(path: '\\Pedigree\\Has multiple babies\\'),
                                new ValueConstraint(Type.NUMERIC, Operator.GREATER_THAN, 0)
                        ]),
                        relationTypeLabel: 'SIB',
                        biological: true,
                ), user)
        then: 'the sisters ids have been returned'
        siblingsWitTwins*.id as Set == [-3002L, -3004L] as Set
    }

}
