package org.transmartproject.db.clinical

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.users.SimpleUser
import spock.lang.Specification

import static org.transmartproject.core.multidimquery.query.Operator.EQUALS

@Rollback
@Integration
class CrossTableServicePgSpec extends Specification {

    public static final TRUE_CONSTRAINT = new TrueConstraint()
    public static final GT_1000_STUDY_CONSTRAINT = new StudyNameConstraint(studyId: 'ORACLE_1000_PATIENT')
    public static final SimpleUser ADMIN = new SimpleUser('admin', null, null, true, [:])

    @Autowired
    CrossTableService crossTableService

    def 'test calculating cross-table subject counts for the whole study'() {
        when:
        def crossTable = crossTableService.retrieveCrossTable(rowConstraints, columnConstraints, subjectConstraint, ADMIN)
        then:
        crossTable.rows == rows

        where:
        rowConstraints                  | columnConstraints | subjectConstraint        | rows
        [TRUE_CONSTRAINT]               | [TRUE_CONSTRAINT] | GT_1000_STUDY_CONSTRAINT | [[1200]]
        [new Negation(TRUE_CONSTRAINT)] | [TRUE_CONSTRAINT] | GT_1000_STUDY_CONSTRAINT | [[0]]
    }


    def 'test calculating cross-table for categorical values'() {
        given:
        def rowConstraints = ['Heart', 'Stomach', 'Head', 'Lung', 'Mouth', 'Liver', 'Breast', 'Arm', 'Leg'].collect {
            studyCategoricalValueConstraint('O1KP:CAT1', it)
        }
        def columnConstraints = ['Female', 'Male'].collect { studyCategoricalValueConstraint('O1KP:GENDER', it) }
        def subjectConstraint = new SubSelectionConstraint(dimension: 'patient', constraint: GT_1000_STUDY_CONSTRAINT)
        when:
        def crossTable = crossTableService.retrieveCrossTable(rowConstraints, columnConstraints, subjectConstraint, ADMIN)
        then:
        crossTable.rows == [[68, 74],
                            [64, 79],
                            [69, 82],
                            [68, 69],
                            [64, 63],
                            [71, 59],
                            [61, 58],
                            [60, 60],
                            [57, 74]]
    }

    static Constraint studyCategoricalValueConstraint(String conceptCode, String conceptValue, String studyId = 'ORACLE_1000_PATIENT') {
        new AndConstraint([
                new AndConstraint([
                        new ConceptConstraint(conceptCode: conceptCode),
                        new StudyNameConstraint(studyId: studyId),
                ]),
                new ValueConstraint(valueType: 'STRING', operator: EQUALS, value: conceptValue)
        ])
    }

}
