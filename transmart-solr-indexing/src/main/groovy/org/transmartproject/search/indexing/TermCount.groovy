package org.transmartproject.search.indexing

import groovy.transform.Immutable

@Immutable
class TermCount implements Comparable<TermCount> {
    String term
    int count

    @Override
    int compareTo(TermCount o) {
        (- (this.count <=> o?.count)) ?: this.term <=> o?.term
    }
}
