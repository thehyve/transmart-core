package org.transmartproject.batch.highdim.mrna.platform

import com.google.common.base.Function
import com.google.common.base.Splitter
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import groovy.transform.Canonical
import org.transmartproject.batch.highdim.datastd.PlatformOrganismSupport

/**
 * Represents a line on the mrna annotations file
 */
@Canonical
class MrnaAnnotationRow implements PlatformOrganismSupport {
    String gplId
    String probeName
    String genes
    String entrezIds
    String organism

    /* genes are originally separated by ' /// '
     * We accept separation by , or 2-3 /'s, with or without whitespace
     */
    private final Splitter splitter = Splitter.on(~/,|\/{2,3}/).trimResults()

    @Lazy
    List<String> geneList = { ->
        if (!genes) {
            return [null]
        }

        Lists.newArrayList(emptyToNull(splitter.splitToList(genes)))
    }()

    @Lazy
    @SuppressWarnings('DuplicateListLiteral')
    List<Long> entrezIdList = { ->
        if (!entrezIds) {
            return [null]
        }

        Lists.newArrayList(
                Iterables.transform(
                        emptyToNull(splitter.split(entrezIds)),
                        { String it -> it?.toLong() } as Function<String, Long>))
    }()

    private Iterable<String> emptyToNull(Iterable<String> original) {
        Iterables.transform(original,
                { it == '' ? null : it } as Function<String, String>)
    }
}
