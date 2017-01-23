package org.transmartproject.batch.batchartifacts

import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepExecution
import org.springframework.batch.item.file.LineCallbackHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.util.Assert
import org.transmartproject.batch.support.StringUtils

/**
 * Is passed the skipped TSV file header and saves it in the step execution
 * context.
 */
@Slf4j
class HeaderParsingLineCallbackHandler implements LineCallbackHandler {

    public static final String IN_KEY = HeaderSavingLineCallbackHandler.KEY
    public static final String PARSED_HEADER_OUT_KEY = IN_KEY + '.parsed'
    public static final String DELIMITER = '.'

    @Value('#{assayMappingsRowStore.allSampleCodes}')
    Set<String> mappingSampleCodes

    Set<String> registeredSuffixes

    @Value('#{stepExecution}')
    StepExecution stepExecution

    @Autowired
    HeaderSavingLineCallbackHandler delegate

    String defaultSuffix

    boolean skipUnmappedData

    @Override
    void handleLine(String line) {
        List<String> columnNames = getColumnNames(line)
        if (!columnNames || columnNames.size() < 2) {
            throw new IllegalStateException('No data columns are found.')
        }
        List<String> sampleColumnNames = columnNames[1..-1]

        Map<String, List<Map<String, String>>> columnsParseOptions = sampleColumnNames.collectEntries { columnName ->
            [(columnName): getAllValidSplitCandidates(columnName)]
        }

        checkUnambiguity(columnsParseOptions)
        Map<String, Map<String, String>> finalColumnsParseOptions = columnsParseOptions.collectEntries {
            [it.key, it.value ? it.value[0] : []]
        }
        validate(finalColumnsParseOptions)
        stepExecution.executionContext.put(PARSED_HEADER_OUT_KEY, finalColumnsParseOptions)
    }

    private void validate(Map<String, Map<String, String>> columnsParseOptions) {
        Set<List<String>> presentSampleSuffixPairs = columnsParseOptions
                .collect { [it.value.sample, it.value.suffix] } as Set

        if (presentSampleSuffixPairs.size() < columnsParseOptions.size()) {
            def dupColumnNames = columnsParseOptions
                    .groupBy { it.value }
                    .findAll { it.value.size() > 1 }
                    .values()*.keySet()
            throw new IllegalStateException('Some columns represent the same information: '
                    + dupColumnNames.join(', '))
        }

        Set<String> presentSamples = presentSampleSuffixPairs.collect { it[0] } as Set
        Set<String> presentSuffixes = presentSampleSuffixPairs.collect { it[1] } as Set

        def mustHaveSampleSuffixPairs = [presentSamples, presentSuffixes].combinations()
        def missingSampleSuffixPairs = mustHaveSampleSuffixPairs - presentSampleSuffixPairs
        if (missingSampleSuffixPairs) {
            throw new IllegalStateException(
                    'Following sample-suffix pairs have to be present in the header:'
                            + missingSampleSuffixPairs*.join(DELIMITER).join(', '))
        }
        if (!skipUnmappedData) {
            def noMappingFileSamples = presentSamples - mappingSampleCodes
            if (noMappingFileSamples) {
                throw new IllegalStateException("Some data file samples (${noMappingFileSamples.join(', ')}) " +
                        "do not match the sample codes in the mapping file, the set of which is: ${mappingSampleCodes}")
            }
        }
        def noDataFileSamples = mappingSampleCodes - presentSamples
        if (noDataFileSamples) {
            throw new IllegalStateException('The data file misses samples that are declared in the mapping file: ' +
                    noDataFileSamples.join(', '))
        }
    }

    private static void checkUnambiguity(Map<String, List<Map<String, String>>> columnsParseOptions) {
        def ambiguousColumnNames = columnsParseOptions.
                findAll { it.value.size() > 1 }
                .keySet()

        if (ambiguousColumnNames) {
            throw new IllegalStateException('Following are ambiguous column names: ' + ambiguousColumnNames.join(', ')
                    + '. There no way to split sample code from the suffix unambiguously.')
        }
    }

    private List<Map<String, String>> getAllValidSplitCandidates(final String columnName) {
        List<Map<String, String>> splitCandidates = []
        if (columnName in mappingSampleCodes) {
            splitCandidates << [sample: columnName, suffix: defaultSuffix]
        }

        splitCandidates.addAll(StringUtils.getAllPossiblePairsSplits(columnName, DELIMITER)
                .findAll { sampleCandidate, suffixCandidate ->
            suffixCandidate in registeredSuffixes
        }.collect { sampleCandidate, suffixCandidate ->
            [sample: sampleCandidate, suffix: suffixCandidate]
        })

        if (!splitCandidates) {
            splitCandidates << [sample: columnName, suffix: defaultSuffix]
        }

        splitCandidates
    }

    private List<String> getColumnNames(final String line) {
        delegate.handleLine(line)

        stepExecution.executionContext.get(IN_KEY) as List
    }

    @Value("#{jobParameters['SKIP_UNMAPPED_DATA']}")
    void setSkipUnmappedData(String value) {
        Assert.isTrue(value in ['Y', 'N'])

        skipUnmappedData = value == 'Y'
    }

}
