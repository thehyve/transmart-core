package org.transmartproject.batch.clinical

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.batch.model.Row
import org.transmartproject.batch.model.WordMapping

/**
 * ItemProcessor of Row that replaces words defined in word_mappings
 */
class WordReplaceItemProcessor implements ItemProcessor<Row, Row> {

    @Autowired
    ClinicalJobContext jobContext

    private Map<String,List<Mapping>> wordMappings

    @Override
    Row process(Row item) throws Exception {
        int count = replaceWords(item)
        if (count > 0) {
            //@todo update StepContribution? log?
        }
        return item
    }

    int replaceWords(Row row) {

        synchronized (this) {
            if (!wordMappings) {
                //lazy instantiated
                wordMappings = getMappings(jobContext.wordMappings)
            }
        }

        List<Mapping> list = wordMappings.get(row.filename)
        int count = 0
        list.each {
            if (it.from == row.values[it.column]) {
                row.values[it.column] = it.to
                count++
            }
        }
        count
    }

    static Map<String,List<Mapping>> getMappings(List<WordMapping> sourceList) {
        Map<String,List<Mapping>> result
        if (sourceList) {
            Map<String, List<WordMapping>> map = sourceList.groupBy { it.filename }
            result = map.collectEntries {
                List<Mapping> mappings = it.value.collect {
                    new Mapping(from: it.originalValue, to: it.newValue, column: it.column)
                }
                [(it.key): mappings]
            }
        } else {
            result = [:]
        }
        result
    }

}

class Mapping {
    String from
    String to
    int column
}
