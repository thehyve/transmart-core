package org.transmartproject.batch.support

/**
 *
 */
class UpdateQueryBuilder {

    String table

    private List<String> columns = []

    private List<String> keys = []

    UpdateQueryBuilder addKeys(String ... keys) {
        this.keys.addAll(keys.toList())
        this
    }

    UpdateQueryBuilder addColumns(String ... columns) {
        this.columns.addAll(columns.toList())
        this
    }

    String toSQL() {
        StringBuilder sb = new StringBuilder("UPDATE $table SET ")
        sb.append(columns.collect { "$it = :$it" } .join(', ') )
        sb.append(' WHERE ')
        sb.append(keys.collect { "$it = :$it" }.join(' AND ') )
        sb.toString()
    }

}
