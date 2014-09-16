package org.transmartproject.batch.clinical

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.model.ConceptNode
import org.transmartproject.batch.model.DemographicVariable
import org.transmartproject.batch.model.Patient
import org.transmartproject.batch.model.Row
import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.support.Keys
import org.transmartproject.batch.support.SequenceReserver
import org.transmartproject.batch.support.Sequences

import javax.annotation.PostConstruct

/**
 * Converts Rows into FactRowSets.
 */
class RowToFactRowSetConverter implements ItemProcessor<Row, FactRowSet> {

    @Autowired
    ClinicalJobContext jobContext

    @Autowired
    SequenceReserver sequenceReserver

    private String studyId

    private Map<String, FileVariables> variablesMap

    @Override
    FactRowSet process(Row item) throws Exception {
        FileVariables vars = variablesMap.get(item.filename)
        Patient patient = getPatient(item, vars)
        vars.create(studyId, item, patient, sequenceReserver)
    }


    @PostConstruct
    void init() {
        sequenceReserver.configureBlockSize(Sequences.PATIENT, 10)
        sequenceReserver.configureBlockSize(Sequences.CONCEPT, 10)

        this.studyId = jobContext.getJobParameters().get(Keys.STUDY_ID)
        Map<String,List<Variable>> map = jobContext.variables.groupBy { it.filename }
        this.variablesMap = map.collectEntries { [(it.key): FileVariables.create(it.value)] }
    }

    private Patient getPatient(Row item, FileVariables vars) {
        Patient patient = jobContext.patientSet.getPatient(vars.getPatientId(item))
        patient.demographicValues.putAll(vars.getDemographicRelatedValues(item))

        if (!patient.code) {
            //new patient: reserve code
            patient.code = sequenceReserver.getNext(Sequences.PATIENT)
            //println "reserved patient code $patient.code"
        }
        patient
    }
}

/**
 * Variables defined for a file
 */
class FileVariables {
    Variable subjectIdVariable
    Variable siteIdVariable
    Variable visitNameVariable
    List<Variable> otherVariables = []
    List<Variable> demographicRelated = []

    static FileVariables create(List<Variable> list) {
        def otherVariables = []
        def args = [:]
        def demographic = []
        list.each {
            switch (it.dataLabel) {
                case Variable.SUBJ_ID:
                    args.put('subjectIdVariable', it)
                    break
                case Variable.SITE_ID:
                    args.put('siteIdVariable', it)
                    break
                case Variable.VISIT_NAME:
                    args.put('visitNameVariable', it)
                    break
                default:
                    otherVariables.add(it)
            }
            if (it.demographicVariable) {
                demographic.add(it)
            }
        }
        args.put('otherVariables', otherVariables)
        args.put('demographicRelated', demographic)

        new FileVariables(args)
    }

    String getPatientId(Row row) {
        row.values.get(subjectIdVariable.columnNumber)
    }

    FactRowSet create(String studyId, Row row, Patient patient, SequenceReserver reserver) {
        FactRowSet result = new FactRowSet()
        result.studyId = studyId
        result.patient = patient

        if (siteIdVariable) {
            result.siteId = row.values.get(siteIdVariable.columnNumber)
        }
        if (visitNameVariable) {
            result.visitName = row.values.get(visitNameVariable.columnNumber)
        }

        otherVariables.each {
            String value = row.values.get(it.columnNumber)
            ConceptNode concept = result.addValue(it, value)
            concept.addSubject(patient.id) //add to counter

            ConceptNode tmp = concept
            //goes up in the concept hierarchy, reserving codes until no longer necessary
            while (tmp && !tmp.code) {
                //new concept: reserve code
                tmp.code = reserver.getNext(Sequences.CONCEPT)
                println "reserved concept code $tmp.code"
                tmp = tmp.parent //recurse to parent
            }
        }

        result
    }

    Map<Variable, Object> getDemographicRelatedValues(Row row) {
        demographicRelated.collectEntries { [(it), row.values[it.columnNumber]] }
    }

}
