package org.transmartproject.batch.clinical

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.model.Patient
import org.transmartproject.batch.model.Row
import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.support.Keys

import javax.annotation.PostConstruct

/**
 * Converts Rows into FactRowSets.
 */
class RowToFactRowSetConverter implements ItemProcessor<Row, FactRowSet> {

    @Autowired
    ClinicalJobContext jobContext

    private String studyId

    private Map<String, FileVariables> variablesMap

    @Override
    FactRowSet process(Row item) throws Exception {
        FileVariables vars = variablesMap.get(item.filename)
        Patient patient = jobContext.patientSet.getPatient(vars.getPatientId(item))
        vars.create(studyId, item, patient)
    }

    @PostConstruct
    void init() {
        this.studyId = jobContext.getJobParameters().get(Keys.STUDY_ID)
        Map<String,List<Variable>> map = jobContext.variables.groupBy { it.filename }
        this.variablesMap = map.collectEntries { [(it.key): FileVariables.create(it.value)] }
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

    static FileVariables create(List<Variable> list) {
        def otherVariables = []
        def args = [:]
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
        }
        args.put('otherVariables', otherVariables)
        new FileVariables(args)
    }

    String getPatientId(Row row) {
        row.values.get(subjectIdVariable.columnNumber)
    }

    FactRowSet create(String studyId, Row row, Patient patient) {
        FactRowSet result = new FactRowSet()
        result.studyId = studyId
        result.patient = patient

        if (siteIdVariable) {
            result.siteId = row.values.get(siteIdVariable.columnNumber)
        }
        if (visitNameVariable) {
            result.visitName= row.values.get(visitNameVariable.columnNumber)
        }

        otherVariables.each {
            String value = row.values.get(it.columnNumber)
            result.addValue(it, value)
        }

        result
    }

}
