package org.transmartproject.batch.db

/**
 * Helper to generate SQL for update statements.
 */
class UpdateQueryBuilder {

    String table

    private final List<String> columns = []

    private final List<String> keys = []

    UpdateQueryBuilder addKeys(String... keys) {
        this.keys.addAll(keys.toList())
        this
    }

    UpdateQueryBuilder addColumns(String... columns) {
        this.columns.addAll(columns.toList())
        this
    }

    String toSQL() {
        StringBuilder sb = new StringBuilder("UPDATE $table SET ")
        sb.append(columns.collect { "$it = :$it" }.join(', '))
        sb.append(' WHERE ')
        sb.append(keys.collect { "$it = :$it" }.join(' AND '))
        sb.toString()
    }

}
