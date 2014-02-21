// configuration for plugin testing - will not be included in the plugin zip

def dataSourceConfig = new File("${userHome}/" +
        ".grails/transmartConfig/DataSource-coredb.groovy")

if (dataSourceConfig.exists()) {
    grails.config.locations = ["file:${dataSourceConfig.getAbsolutePath()}"]
}

grails.databinding.convertEmptyStringsToNull = false
grails.databinding.trimStrings = false

log4j = {
    root {
        info('stdout')
    }
}
