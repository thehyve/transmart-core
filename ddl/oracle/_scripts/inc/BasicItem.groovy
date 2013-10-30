package inc

/**
 * Item with no associated data
 */
class BasicItem extends Item {
    @Override
    String getData() {
        ''
    }

    String toString() {
        "$type ${owner}.$name"
    }
}
