import inc.oracle.CsvLoader
import inc.oracle.SqlProducer
import java.util.Locale;

Locale.setDefault(new Locale("en", "US"))
def sql = SqlProducer.createFromEnv()

System.in.eachLine { String line ->
    def (table, file) = line.split('\t')

    println "Loading ${table} from ${file}"
    try {
        def csvLoader = new CsvLoader(
                sql: sql,
                table: table,
                file: file,
                delimiter: '\t'
        )
        csvLoader.prepareConnection()
        csvLoader.load()
    } catch (Exception exception) {
        exception.printStackTrace(System.err)
        System.exit 1
    }
}
