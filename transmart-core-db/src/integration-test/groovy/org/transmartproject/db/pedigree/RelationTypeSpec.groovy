/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.pedigree

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.Hypercube
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import org.transmartproject.core.multidimquery.query.Operator
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.QueryBuilderException
import org.transmartproject.core.multidimquery.query.RelationConstraint
import org.transmartproject.core.multidimquery.query.Type
import org.transmartproject.core.multidimquery.query.ValueConstraint
import org.transmartproject.core.pedigree.RelationTypeResource
import org.transmartproject.db.i2b2data.PatientMapping
import org.transmartproject.db.user.User
import spock.lang.Specification

import static org.transmartproject.db.multidimquery.DimensionImpl.getPATIENT

@Rollback
@Integration
class RelationTypeSpec extends Specification {

    @Autowired
    MultiDimensionalDataResource multiDimService

    @Autowired
    RelationTypeResource relationTypeResource

    void 'test fetching all relation types'() {
        when: 'fetching all relation types'
        def relationTypes = relationTypeResource.all

        then: 'the result matches the relation types in the test data'
        relationTypes.label as Set<String> == ['PAR', 'CHI', 'SIB', 'DZ', 'MZ', 'COT', 'SPO'] as Set<String>
    }

    void 'test fetching relation type'() {
        when: 'fetching relation type with label MZ'
        def relationType = relationTypeResource.getByLabel('MZ')

        then: 'the relation type object is returned'
        relationType.description == 'Monozygotic twin'
        relationType.symmetrical
        relationType.biological
    }

    void 'test relation domain object mapping'() {
        def subject1 = PatientMapping.findByEncryptedIdAndSource('1', 'SUBJ_ID').patient
        def subject2 = PatientMapping.findByEncryptedIdAndSource('2', 'SUBJ_ID').patient
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
        subjects.collect { it.subjectIds.get('SUBJ_ID') } as Set == ['1', '2', '4', '5', '10', '11'] as Set
    }

    void "get patients applying relation constraint"() {
        def user = User.findByUsername('test-public-user-1')

        when: 'no relation type specified'
        multiDimService.getDimensionElements(PATIENT.name, new RelationConstraint(), user)
        then: 'exception is thrown'
        def e1 = thrown(QueryBuilderException)
        e1.message == 'No null relation type found.'

        when: 'not-existed relation type specified'
        multiDimService.getDimensionElements(PATIENT.name,
                new RelationConstraint(
                        relationTypeLabel: 'NON-EXISTENT'
                ), user)
        then: 'exception is thrown'
        def e2 = thrown(QueryBuilderException)
        e2.message == 'No NON-EXISTENT relation type found.'

        when: 'get parents for the subject'
        def allParents = multiDimService.getDimensionElements(PATIENT.name,
                new RelationConstraint(
                        relationTypeLabel: 'PAR'
                ), user)
        then: 'all parents selected'
        allParents.collect { it.subjectIds.get('SUBJ_ID') } as Set == ['1', '2', '3', '4', '5', '10', '11'] as Set

        when: 'get parents for the subject'
        def parentsForSubject = multiDimService.getDimensionElements(PATIENT.name,
                new RelationConstraint(
                        relatedSubjectsConstraint: new PatientSetConstraint(
                                subjectIds: ['14']
                        ),
                        relationTypeLabel: 'PAR'
                ), user)
        then: 'both parents selected'
        parentsForSubject.collect { it.subjectIds.get('SUBJ_ID') } as Set == ['10', '11'] as Set

        when: 'get all step parents'
        def stepParents = multiDimService.getDimensionElements(PATIENT.name,
                new RelationConstraint(
                        relationTypeLabel: 'PAR',
                        biological: false
                ), user)
        then: 'both step parents selected'
        stepParents.collect { it.subjectIds.get('SUBJ_ID') } as Set == ['4', '5'] as Set

        when: 'get all parents that don\'t live with theirs kids at the same address'
        def parentsThatLiveSeparateFromTheirKids = multiDimService.getDimensionElements(PATIENT.name,
                new RelationConstraint(
                        relationTypeLabel: 'PAR',
                        shareHousehold: false
                ), user)
        then: 'all such parents selected'
        parentsThatLiveSeparateFromTheirKids.collect { it.subjectIds.get('SUBJ_ID') } as Set == ['2', '3', '4', '5'] as Set

        when: 'get all biological siblings that have twin kids.'
        def siblingsWithTwins = multiDimService.getDimensionElements(PATIENT.name,
                new RelationConstraint(
                        relatedSubjectsConstraint: new AndConstraint([
                                new ConceptConstraint(path: '\\Pedigree\\Number of children that are multiplet\\'),
                                new ValueConstraint(Type.NUMERIC, Operator.GREATER_THAN, 0)
                        ]),
                        relationTypeLabel: 'SIB',
                        biological: true,
                ), user)
        then: 'the sisters ids have been returned'
        siblingsWithTwins.collect { it.subjectIds.get('SUBJ_ID') } as Set == ['2', '4'] as Set
    }

}
