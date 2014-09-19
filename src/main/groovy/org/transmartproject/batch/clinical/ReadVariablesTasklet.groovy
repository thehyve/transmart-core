package org.transmartproject.batch.clinical

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.model.ConceptTree
import org.transmartproject.batch.model.DemographicVariable
import org.transmartproject.batch.model.Variable
import org.transmartproject.batch.support.LineListener
import org.transmartproject.batch.support.LineStepContributionAdapter

/**
 * Tasklet that reads the column map file (variables) and populates the variables list
 */
class ReadVariablesTasklet implements Tasklet {

    @Value("#{clinicalJobContext.variables}")
    List<Variable> variables

    @Value("#{clinicalJobContext.conceptTree}")
    ConceptTree conceptTree

    @Value("#{jobParameters['dataLocation']}")
    String dataLocation

    @Value("#{jobParameters['columnMapFile']}")
    String columnMapFile

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        File file = getFile()
        LineListener listener = new LineStepContributionAdapter(contribution)

        List<Variable> list = Variable.parse(file.newInputStream(), listener, conceptTree)
        variables.clear()
        variables.addAll(list)
        //jobContext.variables.clear()
        //jobContext.variables.addAll(list)

        Map<DemographicVariable, Variable> demographicVarMap = [:]

        list.each {
            DemographicVariable var = DemographicVariable.getMatching(it.dataLabel)
            if (var) {
                Variable old = demographicVarMap.get(var)
                if (old) {
                    throw new IllegalArgumentException("Found 2 different variables for demographic $var.name : $old.dataLabel and $it.dataLabel")
                }
                it.demographicVariable = var //sets the associated demographic variable
            }
        }

        return RepeatStatus.FINISHED
    }

    File getFile() {
        if (!dataLocation) {
            throw new IllegalArgumentException('Data location not defined')
        }
        if (!columnMapFile) {
            throw new IllegalArgumentException('Column map file not defined')
        }
        new File(dataLocation, columnMapFile)
    }

}
