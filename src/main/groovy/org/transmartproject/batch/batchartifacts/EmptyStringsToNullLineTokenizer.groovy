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

    private static final Function<String, String> TRANSFORMATION_FUNCTION =
            new EmptyStringToNullFunction()

    @Override
    protected List<String> doTokenize(String line) {
        List<String> superResult = super.doTokenize(line)
        Lists.transform(superResult, TRANSFORMATION_FUNCTION)
    }

    @CompileStatic
    static class EmptyStringToNullFunction implements Function<String, String> {

        @Override
        String apply(String input) {
            input.empty ? null : input
        }
    }
}
