import groovy.sql.Sql

def setupDatabaseConnection() {
  def driver = "oracle.jdbc.driver.OracleDriver"
  def jdbcUrl = "jdbc:oracle:thin:@${System.getenv('ORAHOST')}:${System.getenv('ORAPORT')}:${System.getenv('ORASID')}"
  def username = 'tm_cz'
  def password = 'tm_cz'
  Sql sql = Sql.newInstance jdbcUrl, username, password, driver
  sql
}

def parseOptions() {
  def cli = new CliBuilder(usage: "InsertGplInfo.groovy -p platform -t title -o organism")
  cli.p('which platform', required: true, longOpt: "platform", args: 1)
  cli.t('which title', required: true, longOpt: "title", args: 1)
  cli.o('which organism', required: true, longOpt: "organism", args: 1)
  def options = cli.parse(args)
  options
}


def alreadyLoaded(platform) {
  sql = setupDatabaseConnection()
  result = sql.firstRow(
    "SELECT platform FROM deapp.de_gpl_info WHERE platform = ?", [platform]
  )

  if (result == null) {
    return false
  } else {
    return true
  }
  sql.close()
}

def insertGlpInfo(options) {
  sql = setupDatabaseConnection()
  sql.execute(
      "INSERT INTO deapp.de_gpl_info(platform, title, organism, marker_type)" +
      " VALUES (:platform, :title, :organism, 'Gene Expression')",
      [platform: options.platform, title: options.title, organism: options.organism]
  )
  sql.close()
}

options = parseOptions()
if (!options) return

if (!alreadyLoaded(options.platform)) {
  insertGlpInfo(options)
} else {
  println "Platform ${options.platform} already loaded; skipping"
}
