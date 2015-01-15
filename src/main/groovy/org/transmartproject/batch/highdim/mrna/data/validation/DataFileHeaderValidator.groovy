package org.transmartproject.batch.highdim.mrna.data.validation

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.file.LineCallbackHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.batchartifacts.HeaderSavingLineCallbackHandler

/**
 * Validates the headers found in the data file.
 */
@Component
@StepScope
@CompileStatic
@Slf4j
class DataFileHeaderValidator implements LineCallbackHandler {
    @Value('#{mrnaMappings.allSampleCodes}')
    private Set<String> mappingSampleCodes

    @Value('#{stepExecution}')
    private StepExecution stepExecution

    @Autowired
    private HeaderSavingLineCallbackHandler delegate

    @Override
    void handleLine(String line) {
        delegate.handleLine(line)

        List<String> headers = (List) stepExecution.executionContext.get(
                HeaderSavingLineCallbackHandler.KEY)

        if (headers.size() != mappingSampleCodes.size() + 1) {
            throw new ParseException(
                    "Got header with an unexpected number of fields: " +
                            "${headers.size()}, expected ID_REF plus a " +
                            "number equal to that of the number of samples " +
                            "included in the mapping file " +
                            "(1 + ${mappingSampleCodes.size()})")
        }


        String firstHeader = headers[0]
        if (firstHeader != 'ID_REF') {
            log.warn("Expected first header field to be ID_REF, got $firstHeader")
        }

        headers[1..-1].each { String header ->
            if (!(header in mappingSampleCodes)) {
                throw new ParseException("Data file header field " +
                        "'$header' does not match one of the sample codes " +
                        "in the mapping file, the set of which is: " +
                        "$mappingSampleCodes")
            }
        }
    }
}
