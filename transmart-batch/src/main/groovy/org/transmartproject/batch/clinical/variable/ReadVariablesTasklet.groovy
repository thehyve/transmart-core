package org.transmartproject.batch.clinical.variable

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.concept.ConceptTree
import org.transmartproject.batch.patient.DemographicVariable
import org.transmartproject.batch.support.LineListener
import org.transmartproject.batch.support.LineStepContributionAdapter

import java.nio.file.Files
import java.nio.file.Path

/**
 * Tasklet that reads the column map file (variables) and populates the variables list
 */
class ReadVariablesTasklet implements Tasklet {

    @Value("#{clinicalJobContext.variables}")
    List<ClinicalVariable> variables

    @Value("#{clinicalJobContext.conceptTree}")
    ConceptTree conceptTree

    @Value("#{jobParameters['COLUMN_MAP_FILE']}")
    Path columnMapFile

    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LineListener listener = new LineStepContributionAdapter(contribution)
        List<ClinicalVariable> list = ClinicalVariable.parse(
                Files.newInputStream(columnMapFile),
                listener,
                conceptTree)
        variables.clear()
        variables.addAll(list)

        Map<DemographicVariable, ClinicalVariable> demographicVarMap = [:]

        list.each {
            DemographicVariable var = DemographicVariable.getMatching(it.dataLabel)
            if (var) {
                ClinicalVariable old = demographicVarMap.get(var)
                if (old) {
                    throw new IllegalArgumentException(
                            "Found 2 different variables for demographic " +
                                    "$var.name : $old.dataLabel and $it.dataLabel")
                }
                it.demographicVariable = var //sets the associated demographic variable
            }
        }

        RepeatStatus.FINISHED
    }
}
