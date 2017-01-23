package org.transmartproject.batch.i2b2.codelookup

import com.google.common.collect.Ordering
import com.google.common.collect.SetMultimap
import com.google.common.collect.TreeMultimap
import groovy.transform.CompileStatic
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component

/**
 * Stores code lookups in memory (loaded from i2b2demodata.code_lookup)
 */
@Component
@JobScope
@CompileStatic
class CodeLookupStore {

    private final SetMultimap<Tuple /* table,column */, String /* code */> codes =
            TreeMultimap.create(
                    new Ordering<Tuple>() {
                        @Override
                        int compare(Tuple left, Tuple right) {
                            String leftTable = left.get(0)
                            String rightTable = right.get(0)
                            String leftColumn = left.get(1)
                            String rightColumn = right.get(1)

                            leftTable <=> rightTable ?:
                                    leftColumn <=> rightColumn
                        }
                    },
                    Ordering.natural()
            )

    void add(String table, String column, String code) {
        codes.put(new Tuple(table, column), code)
    }

    Set<String> getCodesFor(String table, String column) {
        codes.get(new Tuple(table, column))
    }
}
