package org.transmartproject.batch.clinical

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.model.Row
import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.support.Keys

import javax.annotation.PostConstruct

/**
 *
 */
class RowToFactRowSetConverter implements ItemProcessor<Row, FactRowSet> {

    @Autowired
    ClinicalJobContext jobContext

    private String studyId

    private Map<String, FileVariables> variablesMap

    @Override
    FactRowSet process(Row item) throws Exception {
        FileVariables vars = variablesMap.get(item.filename)
        vars.create(studyId, item)
    }

    @PostConstruct
    void init() {
        this.studyId = jobContext.getJobParameters().get(Keys.STUDY_ID)
        //@todo init variables per file (make sure post construct is the right moment)
        Map<String,List<Variable>> map = jobContext.variables.groupBy { it.filename }
        this.variablesMap = map.collectEntries { [(it.key): FileVariables.create(it.value)] }
    }

}

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

    FactRowSet create(String studyId, Row row) {
        Map args = [studyId: studyId]

        args.put('studyId', st)
        args.put('subjectId': row.values.get(subjectIdVariable.columnNumber))
        args.put('variableValueMap': otherVariables.collectEntries { [(it): row.values.get(it.columnNumber)] })
        if (siteIdVariable) {
            args.put('siteId': row.values.get(siteIdVariable.columnNumber))
        }
        if (visitNameVariable) {
            args.put('visitName': row.values.get(visitNameVariable.columnNumber))
        }
        new FactRowSet(args)
    }

}
