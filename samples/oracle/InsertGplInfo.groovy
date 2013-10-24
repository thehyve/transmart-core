import groovy.sql.Sql
import DatabaseConnection

def parseOptions() {
  def cli = new CliBuilder(usage: "InsertGplInfo.groovy -p platform -t title -o organism")
  cli.p('which platform', required: true, longOpt: "platform", args: 1)
  cli.t('which title', required: true, longOpt: "title", args: 1)
  cli.o('which organism', required: true, longOpt: "organism", args: 1)
  def options = cli.parse(args)
  options
}


def alreadyLoaded(platform) {
  sql = DatabaseConnection.setupDatabaseConnection()
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
  sql = DatabaseConnection.setupDatabaseConnection()
  sql.execute(
      "INSERT INTO deapp.de_gpl_info(platform, title, organism, marker_type)" +
      " VALUES (:platform, :title, :organism, 'Gene Expression')",
      [platform: options.platform, title: options.title, organism: options.organism]
  )
  sql.close()
}

options = parseOptions()
if (!options) {
	System.exit 1
}

if (!alreadyLoaded(options.platform)) {
  insertGlpInfo(options)
} else {
  println "Platform ${options.platform} already loaded; skipping"
  System.exit 3
}

// vim: et sts=0 sw=2 ts=2 cindent cinoptions=(0,u0,U0
