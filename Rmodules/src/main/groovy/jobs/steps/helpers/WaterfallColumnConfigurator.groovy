package jobs.steps.helpers

import jobs.table.Column
import jobs.table.columns.TransformColumnDecorator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.InvalidArgumentsException

import javax.annotation.PostConstruct

@Component
@Scope('job')
class WaterfallColumnConfigurator extends ColumnConfigurator {

    String keyForOperatorForLow
    String keyForLowValue
    String keyForOperatorForHigh
    String keyForHighValue

    @Lazy
    Closure<Boolean> testerForLow = createTester(
            getStringParam(keyForOperatorForLow),
            getStringParam(keyForLowValue))

    @Lazy
    Closure<Boolean> testerForHigh = createTester(
            getStringParam(keyForOperatorForHigh),
            getStringParam(keyForHighValue))

    @Autowired
    NumericColumnConfigurator numericColumnConfigurator

    @PostConstruct
    void init() {
        numericColumnConfigurator.header = header
        numericColumnConfigurator.alwaysClinical = true
    }

    @Override
    protected void doAddColumn(Closure<Column> decorateColumn) {
        numericColumnConfigurator.doAddColumn(compose(
                { Column originalColumn ->
                    new TransformColumnDecorator(
                            inner: originalColumn,
                            valueFunction: { originalValue ->
                                if (testerForLow(originalValue)) {
                                    [LOW: originalValue]
                                } else if (testerForHigh(originalValue)) {
                                    [HIGH: originalValue]
                                } else {
                                    [BASE: originalValue]
                                }
                            }
                    )
                },
                decorateColumn
        ))

    }

    void setProperty(String name, Object value) {
        boolean found = false

        if (hasProperty(name)) {
            def method = "set${name.capitalize()}"
            this."$method"(value)
            found = true
        }
        if (numericColumnConfigurator.hasProperty(name)) {
            numericColumnConfigurator."$name" = value
            found = true
        }

        if (!found) {
            throw new MissingPropertyException(name, value.getClass())
        }
    }

    Closure<Boolean> createTester(String operator, String valueString) {
        def value = valueString as BigDecimal
        switch (operator) {
            case '<':
                return { it -> it < value }
            case '<=':
                return { it -> it <= value }
            case '>':
                return { it -> it > value }
            case '>=':
                return { it -> it >= value }
            case '=':
                return { it -> it == value }
            default:
                throw new InvalidArgumentsException("Bad value for operator: $operator")
        }
    }

}
