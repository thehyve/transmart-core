package org.transmartproject.batch.clinical

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.transmartproject.batch.model.Row
import org.transmartproject.batch.model.WordMapping

import javax.annotation.PostConstruct

/**
 * ItemProcessor of Row that replaces words defined in word mappings list
 */
class WordReplaceItemProcessor implements ItemProcessor<Row, Row> {

    @Value("#{clinicalJobContext.wordMappings}")
    List<WordMapping> wordMappings

    private Map<String,List<Mapping>> map

    @Override
    Row process(Row item) throws Exception {
        int count = replaceWords(item)
        if (count > 0) {
            //println "replaced $count words"
            //@todo update StepContribution? log?
        }
        return item
    }

    int replaceWords(Row row) {
        List<Mapping> list = map.get(row.filename)
        int count = 0
        list.each {
            if (it.from == row.values[it.column]) {
                row.values[it.column] = it.to
                count++
            }
        }
        count
    }

    @PostConstruct
    void initMap() {
        map = getMappings(wordMappings)
    }

    static Map<String,List<Mapping>> getMappings(List<WordMapping> sourceList) {
        if (!sourceList) {
            return [:]
        }

        Map<String, List<WordMapping>> map = sourceList.groupBy { it.filename }

        return map.collectEntries {
            List<Mapping> mappings = it.value.collect {
                new Mapping(from: it.originalValue, to: it.newValue, column: it.column)
            }
            [(it.key): mappings]
        }
    }

}

class Mapping {
    String from
    String to
    int column
}
