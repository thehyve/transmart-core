package org.transmartproject.batch.clinical.facts

import com.google.common.collect.ImmutableTable
import com.google.common.collect.Table
import groovy.transform.Immutable
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Value

/**
 * ItemProcessor of Row that replaces words defined in word mappings list
 */
@Slf4j
@TypeChecked
class WordReplaceItemProcessor implements ItemProcessor<ClinicalDataRow, ClinicalDataRow> {

    @Value("#{clinicalJobContext.wordMappings}")
    List<WordMapping> wordMappings

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private Table<FileColumn, String, String> mappingTable = calculateMappingTable()

    @Override
    ClinicalDataRow process(ClinicalDataRow item) throws Exception {
        int count = replaceWords(item)
        if (log.debugEnabled && count > 0) {
            log.debug "Replaced $count words in row ${item.index} of ${item.filename}"
        }
        item
    }

    private int replaceWords(ClinicalDataRow row) {
        int count = 0
        row.values.eachWithIndex { String v, def /* intellij bug */ column ->
            def repl = mappingTable.get(
                    fileColumn(row.filename, (Integer) column),
                    v)
            if (repl == null) {
                return
            }
            if (repl != '') {
                log.trace "replacement on ${row.filename}" +
                        "[${row.index}:${column}] $v -> $repl"
                row.values[(Integer) column] = repl
            } else {
                log.trace "replacement on ${row.filename}" +
                        "[${row.index}:${column}] $v -> [DELETED OBSERVATION]"
                row.values[(Integer) column] = (String) null
            }
            count++
        }

        count
    }

    private Table<FileColumn, String, String> calculateMappingTable() {
        if (!wordMappings) {
            return ImmutableTable.of()
        }

        def builder = ImmutableTable.<FileColumn, String, String> builder()
        wordMappings.each { WordMapping mapping ->
            builder.put(
                    fileColumn(mapping.filename, mapping.column),
                    mapping.originalValue,
                    mapping.newValue)
        }
        builder.build()
    }

    private FileColumn fileColumn(String file, Integer column) {
        fileColumnCache.call([file, column] as Object[])
    }

    Closure<FileColumn> fileColumnCache = { String file, Integer column ->
        new FileColumn(file, column)
    }.memoizeAtMost(100)

    @Immutable
    private static final class FileColumn {
        String file
        int column /* 1-based */
    }
}
