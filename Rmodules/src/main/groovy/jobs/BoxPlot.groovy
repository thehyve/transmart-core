package jobs

import jobs.steps.helpers.BoxPlotVariableColumnConfigurator
import jobs.table.columns.PrimaryKeyColumn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidArgumentsException

@Component
@Scope('job')
class BoxPlot extends CategoricalOrBinnedJob {

    @Autowired
    BoxPlotVariableColumnConfigurator independentVariableConfigurator

    @Autowired
    BoxPlotVariableColumnConfigurator dependentVariableConfigurator

    @Override
    void afterPropertiesSet() throws Exception {
        primaryKeyColumnConfigurator.column =
                new PrimaryKeyColumn(header: 'PATIENT_NUM')

        configureConfigurator independentVariableConfigurator, '', 'independent'
        configureConfigurator dependentVariableConfigurator,   '', 'dependent'

        independentVariableConfigurator.valueForThisColumnBeingBinned = 'IND'
        independentVariableConfigurator.keyForIsCategorical           = 'independentVariableCategorical'
        dependentVariableConfigurator.valueForThisColumnBeingBinned   = 'DEP'
        dependentVariableConfigurator.keyForIsCategorical             = 'dependentVariableCategorical'

        validateDataTypes()
    }

    void validateDataTypes() {
        // we don't usually validate these things, but the frontend is not
        // validating this right now and it's causing confusion
        if (independentVariableConfigurator.categoricalOrBinned &&
                dependentVariableConfigurator.categorical) {
            throw new InvalidArgumentsException(
                    'Both variables are categorical or binned continuous')
        }
        if (!independentVariableConfigurator.categoricalOrBinned &&
                !dependentVariableConfigurator.categoricalOrBinned) {
            throw new InvalidArgumentsException('Both variables are unbinned continuous')
        }
    }

    @Override
    protected List<String> getRStatements() {
        [
           '''source('$pluginDirectory/ANOVA/BoxPlotLoader.R')''',
           '''
            BoxPlot.loader(
                    input.filename               = '$inputFileName',
                    concept.dependent            = '$dependentVariable',
                    concept.independent          = '$independentVariable',
                    flipimage                    = as.logical('$flipImage'),
                    concept.dependent.type       = '$divDependentVariableType',
                    concept.independent.type     = '$divIndependentVariableType',
                    genes.dependent              = '$divDependentPathwayName',
                    genes.independent            = '$divIndependentPathwayName',
                    binning.enabled              = '$binning',
                    aggregate.probes.independent = '$divIndependentVariableprobesAggregation' == 'true',
                    aggregate.probes.dependent   = '$divDependentVariableprobesAggregation'   == 'true',
                    binning.variable             = '$binVariable')'''
        ]
    }

    @Override
    protected getForwardPath() {
        "/boxPlot/boxPlotOut?jobName=$name"
    }

}
