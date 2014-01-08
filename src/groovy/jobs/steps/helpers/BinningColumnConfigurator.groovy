package jobs.steps.helpers

import com.google.common.collect.Maps
import jobs.table.Column
import jobs.table.columns.ColumnDecorator
import jobs.table.columns.binning.CategoricalBinningColumnDecorator
import jobs.table.columns.binning.EvenDistributionBinningColumnDecorator
import jobs.table.columns.binning.EvenSpacedBinningColumnDecorator
import jobs.table.columns.binning.NumericBinRange
import jobs.table.columns.binning.NumericManualBinningColumnDecorator
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidArgumentsException

@Component
@Scope('prototype')
class BinningColumnConfigurator extends ColumnConfigurator {

    String keyForDoBinning,
           keyForManualBinning,
           keyForNumberOfBins,
           keyForBinDistribution, //only if not manual, and continuous
                                  //EDP (evenly distribute population)/ESP (evenly spaced bins)
           keyForBinRanges,       //only if manual
                                  //{bin name 1},{lower},{upper}|{bin name 2},{lower},{upper}|...
                                  //{bin name 1}<>{concept path 1}<>{concept path 2}<>...|...
           keyForVariableType     //only if manual

    ColumnConfigurator innerConfigurator

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {
        innerConfigurator.addColumn(
                compose(decorateColumn, createDecoratorClosure()))
    }

    private Closure<Column> createDecoratorClosure() {
        if (!getStringParam(keyForDoBinning).equalsIgnoreCase('true')) {
            Closure.IDENTITY
        } else {
            def decorator = createBinningDecorator()
            return { Column originalColumn ->
                decorator.inner = originalColumn
                decorator
            }
        }
    }

    private ColumnDecorator createBinningDecorator() {
        if (manualBinning) {
            if (variableContinuous) {
                new NumericManualBinningColumnDecorator(
                        binRanges: createBinRanges())
            } else {
                new CategoricalBinningColumnDecorator(
                        transformationMap: createConceptPathMap())
            }
        } else {
            String distribution = getStringParam keyForBinDistribution
            if (distribution == 'EDP') {
                new EvenDistributionBinningColumnDecorator(
                        numberOfBins: getStringParam(keyForNumberOfBins) as int)
            } else if (distribution == 'ESB') {
                new EvenSpacedBinningColumnDecorator(
                        numberOfBins: getStringParam(keyForNumberOfBins) as int)
            } else {
                throw new InvalidArgumentsException(
                        "Invalid value for $keyForBinDistribution: " +
                        "expected 'EDP' or 'ESB', got $distribution")
            }
        }
    }

    private List<NumericBinRange> createBinRanges() {
        String binRangesString = getStringParam keyForBinRanges

        binRangesString.split(/\|/).collect {
            String[] parts = it.split(',')
            if (parts.length != 3) {
                throw new InvalidArgumentsException("Invalid bin range specification: $it. " +
                        "Complete value: $binRangesString")
            }

            new NumericBinRange(
                    from: parts[1] as BigDecimal,
                    to:   parts[2] as BigDecimal)
        }.sort { it.from }
    }

    private Map<String, String> createConceptPathMap() {
        String binsString = getStringParam keyForBinRanges
        Map result = Maps.newHashMap()

        binsString.split(/\|/).each {
            String[] paths = it.split('<>')
            for (int i = 1 /* pass bin name */; i < paths.length; i++) {
                result[paths[i]] = paths[0]
            }
        }

        result
    }

    boolean isVariableContinuous() { //aka numeric
        params[keyForVariableType] != 'Categorical'
    }

    boolean isManualBinning() {
        getStringParam(keyForManualBinning).equalsIgnoreCase 'TRUE'
    }
}
