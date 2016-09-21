package org.transmartproject.batch.highdim.datastd

import com.google.common.collect.Multiset
import com.google.common.collect.TreeMultiset
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
    @Value('#{assayMappingsRowStore.allSampleCodes}')
    private Set<String> mappingSampleCodes

    @Value('#{stepExecution}')
    private StepExecution stepExecution

    @Autowired
    private HeaderSavingLineCallbackHandler delegate

    boolean skipUnmappedData

    @Override
    void handleLine(String line) {
        delegate.handleLine(line)

        List<String> headers = (List) stepExecution.executionContext.get(
                HeaderSavingLineCallbackHandler.KEY)

        if (headers.size() < 1) {
            throw new ParseException("Got empty header. Unacceptable")
        }

        String firstHeader = headers[0]
        if (firstHeader != 'ID_REF') {
            log.warn("Expected first header field to be ID_REF, got $firstHeader")
        }

        headers[1..-1].each { String header ->
            if (!skipUnmappedData && !(header in mappingSampleCodes)) {
                throw new ParseException("Data file header field " +
                        "'$header' does not match one of the sample codes " +
                        "in the mapping file, the set of which is: " +
                        "$mappingSampleCodes")
            }
        }

        Multiset<String> entriesMultiSet = TreeMultiset.create(headers[1..-1])
        mappingSampleCodes.each { sampleCode ->
            def count = entriesMultiSet.count(sampleCode)
            if (count != 1) {
                throw new ParseException("Expected a header entry " +
                        "'$sampleCode' to be present exactly once, " +
                        "got count $count")
            }
        }
    }

    @Value("#{jobParameters['SKIP_UNMAPPED_DATA']}")
    void setSkipUnmappedData(String value) {
        skipUnmappedData = value == 'Y'
    }
}
