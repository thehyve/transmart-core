import inc.oracle.SqlProducer

Locale.setDefault(new Locale("en", "US"))
def sql = SqlProducer.createFromEnv()
List<String> tables = []
System.in.eachLine { String line ->
    def (table, file) = line.split('\t')
    if (!tables.contains(table)) {
        tables << table
    }
}
tables.reverse().each { String table ->
    println "Deleting ${table}"
    sql.executeUpdate('DELETE FROM ' + table)
}
sql.commit()
