transmart-metacore-plugin is a grails plugin to enable Metacore capabilities within TranSMART
This plugin is not built separately; it is built together with the other plugins when
transmartApp is built.

To enable the transmart-metacore-plugin, edit the Config.groovy file (found in ~/grails/tranmartConfig directory.)

At the end of the file, add a line:
com.thomsonreuters.transmart.metacoreAnalyticsEnable=true

or uncomment the line and change the value from 'false' to true.

