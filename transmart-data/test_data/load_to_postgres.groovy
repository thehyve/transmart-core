import org.postgresql.copy.CopyManager

import java.sql.Connection
import java.sql.DriverManager

import static java.lang.System.getenv

@GrabConfig(systemClassLoader = true)
@Grab('org.postgresql:postgresql:9.3-1100-jdbc4')

String host = getenv('PGHOST')
if (!host || host == '/tmp') {
    host = 'localhost'
}
String url = "jdbc:postgresql://${host}:${getenv('PGPORT')}/${getenv('PGDATABASE')}"
Connection connection = DriverManager.getConnection(url,
        'tm_cz',
        getenv('TM_CZ_PWD') ?: 'tm_cz')

CopyManager copyManager = new CopyManager(connection);

System.in.eachLine { String line ->
    def (table, file) = line.split('\t')
    new File(file).withReader { reader ->
        println "Loading ${table} from ${file}"
        copyManager.copyIn("COPY ${table} FROM STDIN CSV DELIMITER E'\t'", reader)
    }
}
