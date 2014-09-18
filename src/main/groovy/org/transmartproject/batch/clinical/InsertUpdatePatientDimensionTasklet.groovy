package org.transmartproject.batch.clinical

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.transmartproject.batch.model.DemographicVariable
import org.transmartproject.batch.model.Patient
import org.transmartproject.batch.model.PatientSet
import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.support.UpdateQueryBuilder

import javax.annotation.PostConstruct

/**
 *
 */
class InsertUpdatePatientDimensionTasklet implements Tasklet {

    @Autowired
    JdbcTemplate jdbcTemplate

    @Value("#{jobParameters['studyId']}")
    String studyId

    @Value("#{clinicalJobContext.patientSet}")
    PatientSet patientSet

    @Value("#{clinicalJobContext.variables}")
    List<Variable> variables

    private NamedParameterJdbcTemplate namedTemplate

    private SimpleJdbcInsert insert

    private Map<DemographicVariable, Variable> demographicVariableMap

    private String updateSql

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        List<Map> toInsert = []
        List<Map> toUpdate = []

        Date now = new Date()

        patientSet.patientMap.values().each { Patient p ->

            Map<String,Object> map = [
                    patient_num: p.code,
                    update_date: now,
            ]
            //add all the demographic values
            map.putAll(getDemographicValues(p))

            if (p.isNew) {
                map.put('download_date', now)
                map.put('import_date', now)
                map.put('sourcesystem_cd', getSourceSystem(studyId, p.id))
                toInsert.add(map)
            } else {
                toUpdate.add(map)
            }
        }

        if (toUpdate.size() > 0) {
            int[] result = namedTemplate.batchUpdate(updateSql, toUpdate as Map[])
            if (!result.toList().every { it == 1 }) {
                throw new RuntimeException('Updated rows mismatch while updating patient_dimension')
            }
            contribution.incrementWriteCount(toUpdate.size())
        }

        if (toInsert.size() > 0) {
            int[] result = insert.executeBatch(toInsert as Map[])
            if (!result.toList().every { it == 1 }) {
                throw new RuntimeException('Updated rows mismatch while inserting patient_dimension')
            }

            contribution.incrementWriteCount(toInsert.size())
        }

        println contribution

        return RepeatStatus.FINISHED
    }

    Map<String, Object> getDemographicValues(Patient patient) {
        demographicVariableMap.collectEntries {
            DemographicVariable dv = it.key
            Variable var = it.value
            Object value = var ? patient.getDemographicValue(var) : dv.defaultValue
            [ (dv.column), value ]
        }
    }

    @PostConstruct
    void init() {
        namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate)

        insert = new SimpleJdbcInsert(jdbcTemplate)
        insert.withSchemaName('i2b2demodata')
        insert.withTableName('patient_dimension')

        demographicVariableMap = [:]
        variables.each {
            if (it.demographicVariable) {
                demographicVariableMap.put(it.demographicVariable, it)
            }
        }

        List<DemographicVariable> remaining = new ArrayList<>(DemographicVariable.values().toList())
        remaining.removeAll(demographicVariableMap.keySet())

        remaining.each {
            demographicVariableMap.put(it, null) //add remaining demographic vars
        }

        UpdateQueryBuilder builder = new UpdateQueryBuilder(table: 'i2b2demodata.patient_dimension')
        builder.addKeys('patient_num')
        builder.addColumns('update_date')
        builder.addColumns(DemographicVariable.values()*.column as String[])

        updateSql = builder.toSQL()
    }

    static String getSourceSystem(String study, String patientId) {
        "$study:$patientId"
    }

}
