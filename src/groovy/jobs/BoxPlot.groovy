package jobs

import jobs.steps.helpers.BinningColumnConfigurator
import jobs.steps.helpers.ColumnConfigurator
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope('job')
class BoxPlot extends CategoricalOrBinnedJob {

    @Override
    void afterPropertiesSet() throws Exception {
        primaryKeyColumnConfigurator.column =
                new PrimaryKeyColumn(header: 'PATIENT_NUM')

        //both configurators use the same generics parameters...
        configureConfigurator independentVariableConfigurator, '', '', 'X'
        configureConfigurator dependentVariableConfigurator, '', '', 'Y'

        //... but at most one of them is enabled at any moment
        String binVariable = params['binVariable'] as String
        independentVariableConfigurator.binningConfigurator.enabled = ('IND' == binVariable)
        dependentVariableConfigurator.binningConfigurator.enabled = ('DEP' == binVariable)

        //adjusting some props
        independentVariableConfigurator.keyForConceptPaths = 'independentVariable'
        dependentVariableConfigurator.keyForConceptPaths = 'dependentVariable'
        independentVariableConfigurator.keyForDataType = 'divIndependentVariableType'
        dependentVariableConfigurator.keyForDataType = 'divDependentVariableType'
    }

    @Override
    protected List<String> getRStatements() {
        [
           '''source('$pluginDirectory/ANOVA/BoxPlotLoader.R')''',
           '''
            BoxPlot.loader(
                    input.filename           = 'outputfile',
                    concept.dependent        = '$dependentVariable',
                    concept.independent      = '$independentVariable',
                    flipimage                = as.logical('$flipImage'),
                    concept.dependent.type   = '$divDependentVariableType',
                    concept.independent.type = '$divIndependentVariableType',
                    genes.dependent          = '$divDependentPathwayName',
                    genes.independent        = '$divIndependentPathwayName',
                    binning.enabled          = '$binning',
                    binning.variable         = '$binVariable')'''
        ]
    }

    @Override
    protected getForwardPath() {
        "/boxPlot/boxPlotOut?jobName=$name"
    }

}
