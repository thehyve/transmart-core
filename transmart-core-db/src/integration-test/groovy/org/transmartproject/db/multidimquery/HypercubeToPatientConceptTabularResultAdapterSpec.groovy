package org.transmartproject.db.multidimquery

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Ignore
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.multidimquery.MultiDimensionalDataResource
import spock.lang.Specification

//TODO Add missing values support
//TODO Does spss writer require data type?
//TODO Store subject id as an observation?
@Rollback
@Integration
@Ignore
class HypercubeToPatientConceptTabularResultAdapterSpec extends Specification {

    @Autowired
    MultiDimensionalDataResource multiDimService

    def 'tabular result meets the contract'() {
        /*def user = User.findByUsername('test-public-user-1')
        Constraint constraint = new StudyNameConstraint(studyId: "SURVEY1")
        Hypercube hypercube = multiDimService.retrieveClinicalData(constraint, user)
        def adapter = new HypercubeToPatientConceptTabularResultAdapter(hypercube)
        //TODO implement on serialization level
        //adapter.headerRenamingMap = [patient: 'SubjectNumber', study: 'Survey']
        //adapter.excludedDimensionNames = ['start time']

        when:
        List<VariableDataColumn> variableDataColumns = adapter.indicesList
        then:
        variableDataColumns != null
        variableDataColumns*.name as Set == ['start time', 'study', 'patient', 'birthdate', 'gender'] as Set

        when:
        def study = variableDataColumns.find { it.name == 'study' }
        then:
        study.type == SpssType.NUMERIC
        study.measure == Measure.NOMINAL

        when:
        def patient = variableDataColumns.find { it.name == 'patient' }
        then:
        patient.type == SpssType.NUMERIC
        patient.measure == Measure.NOMINAL

        when:
        def gender = variableDataColumns.find { it.name == 'gender' }
        then:
        gender.type == SpssType.NUMERIC
        gender.width == 12
        gender.columns == 14
        gender.measure == Measure.NOMINAL
        gender.valueLabels.size() == 2
        gender.valueLabels[1D] == 'female'
        gender.valueLabels[2D] == 'male'
        gender.missingValues == [-1D, -2D]

        when:
        def birthDate = variableDataColumns.find { it.name == 'birthdate' }
        then:
        birthDate.type == SpssType.DATE
        birthDate.width == 11
        birthDate.columns == 11
        birthDate.measure == Measure.SCALE

        when:
        Dimension patientDimension = hypercube.dimensions.find { it instanceof PatientDimension }
        List<CaseDataRow> rows = adapter.iterator().toList()
        then:
        rows.size() == hypercube.dimensionElements(patientDimension).size()*/
    }

}
