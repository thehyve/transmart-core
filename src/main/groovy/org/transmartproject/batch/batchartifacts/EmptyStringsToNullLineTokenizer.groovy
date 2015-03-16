package org.transmartproject.batch.batchartifacts

import com.google.common.base.Function
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer

/**
 * Extends {@link DelimitedLineTokenizer} to transform empty tokens into nulls.
 */
@CompileStatic
class EmptyStringsToNullLineTokenizer extends DelimitedLineTokenizer {

    @Override
    protected List<String> doTokenize(String line) {
        List<String> superResult = super.doTokenize(line)
        Lists.transform(superResult, { String s ->
            s.empty ? null : s
        } as Function<String, String>)
    }
}
