package inc

import groovy.transform.EqualsAndHashCode
import org.codehaus.jackson.annotate.JsonIgnore

@EqualsAndHashCode(includes = ['type', 'owner', 'name'])
abstract class Item implements Comparable<Item> {
    String type
    String owner
    String name /* null if not named */

    @JsonIgnore
    abstract String getData()

    @JsonIgnore
    String getNameLower() {
        name?.toLowerCase(Locale.ENGLISH)
    }

    int compareTo(Item other) {
        owner <=> other.owner ?: name <=> other.name ?: type <=> other.type
    }
}
