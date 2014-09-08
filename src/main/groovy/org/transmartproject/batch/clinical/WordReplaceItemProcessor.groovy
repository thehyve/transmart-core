package org.transmartproject.batch.clinical

import org.springframework.batch.item.ItemProcessor
import org.transmartproject.batch.model.Row

/**
 *
 */
class WordReplaceItemProcessor implements ItemProcessor<Row, Row> {

    @Override
    Row process(Row item) throws Exception {
        return item
    }

}
