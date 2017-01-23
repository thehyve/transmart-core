package org.transmartproject.batch.i2b2.fact

import groovy.transform.CompileStatic
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.transmartproject.batch.i2b2.fact.FactDataType.*

/**
 * Factory for {@link FactValue} objects.
 *
 * Thread-safe.
 */
@Component
@Scope('singleton')
@CompileStatic
class FactFactory {

    private final static Pattern NUMBER_PATTERN = Pattern.compile '''(?xs)
        (?<operator><=|<|!=|=|>=|>)?
        (?<number>[^|]+)
        (?:|(?<flag>).+)?'''

    private final BlockingQueue<Matcher> numberMatchers = new LinkedBlockingQueue()

    FactValue create(FactDataType type, String data) {
        if (type.is(NUMBER)) {
            createNumberFactValue data
        } else if (type.is(TEXT)) {
            new TextFactValue(
                    textValue: data,
                    // valueFlag not supported for text values
            )
        } else if (type.is(BLOB)) {
            new BlobFactValue(
                    blob: data,
                    // valueFlag to signal encryption not supported
            )
        } else if (type.is(NLP)) {
            new NLPFactValue(
                    blob: data,
                    // valueFlag to signal encryption not supported
            )
        } else {
            throw new IllegalArgumentException("Unrecognized data type: $type")
        }
    }

    NumberFactValue createNumberFactValue(String data) {
        Matcher m = numberMatchers.poll()
        if (m == null) {
            m = NUMBER_PATTERN.matcher(data)
        } else {
            m.reset(data)
        }

        if (!m.matches()) {
            throw new NumberFormatException(
                    "Invalid format for number in i2b2 data file: $data")
        }

        try {
            BigDecimal number =
                    new BigDecimal(m.group('number')) // can throw

            String operatorString = m.group('operator')
            NumberFactValue.NumberFactOperator operator
            if (operatorString != null) {
                operator = NumberFactValue.NumberFactOperator.
                        forString(operatorString)
            }

            String valueFlagString = m.group('flag')
            ValueFlag valueFlag
            if (valueFlagString != null) {
                valueFlag = valueFlag.forString(valueFlagString)
                if (!valueFlag) {
                    throw new IllegalArgumentException(
                            "Invalid value flag: $valueFlagString")
                }
            }

            new NumberFactValue(
                    operator: operator,
                    numberValue: number,
                    valueFlag: valueFlag)
        } finally {
            numberMatchers.offer(m)
        }
    }
}
