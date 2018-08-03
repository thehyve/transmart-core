package org.transmartproject.db.clinical

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.users.SimpleUser
import spock.lang.Specification

@Rollback
@Integration
class CrossTableServicePgSpec extends Specification {

    public static final TRUE_CONSTRAINT = new TrueConstraint()
    public static final GT_1000_STUDY_CONSTRAINT = new StudyNameConstraint(studyId: 'ORACLE_1000_PATIENT')
    public static final SimpleUser ADMIN = new SimpleUser('admin', null, null, true, [:])

    @Autowired
    CrossTableService crossTableService

    def 'counts per study and concept has been enabled'() {
        when:
        def crossTable = crossTableService.retrieveCrossTable(rowConstraints, columnConstraints, subjectConstraint, user)
        then:
        crossTable.rows == rows

        where:
        rowConstraints    | columnConstraints | subjectConstraint        | user  | rows
        [TRUE_CONSTRAINT] | [TRUE_CONSTRAINT] | GT_1000_STUDY_CONSTRAINT | ADMIN | [[1200L]]
    }

}
