package org.transmartproject.db.multidimquery

import org.transmartproject.db.multidimquery.query.*
import org.transmartproject.db.ontology.I2b2Secure
import org.transmartproject.db.tree.TreeNodeImpl
import spock.lang.Specification

import java.time.Instant

class ConstraintSerialiserSpec extends Specification {

    void 'test disjunction of concept constraints'() {
        given: 'a disjunction and its expected serialised form'
        Constraint constraint = new OrConstraint([
                new ConceptConstraint('height'),
                new ConceptConstraint('birthdate')
        ])

        String expected = '{"type":"or","args":[{"type":"concept","conceptCode":"height"},{"type":"concept","conceptCode":"birthdate"}]}'

        when: 'serialising the constraint'
        def result = ConstraintSerialiser.toJson(constraint)

        then: 'the result is equal to the expected JSON'
        result == expected
    }

    void 'test conjunction of study constraint and concept constraint'() {
        given: 'a conjunction and its expected serialised form'
        Constraint constraint = new AndConstraint([
                new StudyNameConstraint('SURVEY1'),
                new ConceptConstraint('height')
        ])

        String expected = '{"type":"and","args":[{"type":"study_name","studyId":"SURVEY1"},{"type":"concept","conceptCode":"height"}]}'

        when: 'serialising the constraint'
        def result = ConstraintSerialiser.toJson(constraint)

        then: 'the result is equal to the expected JSON'
        result == expected
    }

    void 'test date serialisation'() {
        given: 'a time constraint'
        String date = '2017-03-25T13:37:17.783Z'
        Constraint constraint = new TimeConstraint(new Field('value', Type.NUMERIC, 'numValue'),
                Operator.BEFORE, [Date.from(Instant.parse(date))])

        String expected = '{"type":"time","field":{"dimension":"value","type":"NUMERIC","fieldName":"numValue"},"operator":"<-","values":["' + date + '"]}'

        when: 'serialising the constraint'
        def result = ConstraintSerialiser.toJson(constraint)

        then: 'the constraint is properly serialised'
        result == expected
    }

}
