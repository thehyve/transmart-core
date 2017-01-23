//GWAS Related Configuration:
com.recomdata.dataUpload.appTitle="Upload data to tranSMART (GPL)"
com.recomdata.dataUpload.templates.dir="/home/transmart/templates/"
com.recomdata.dataUpload.uploads.dir="/vol1/transmart/uploads/"
com.recomdata.dataUpload.adminEmail="mailto:DL-tranSMART_Support@transmartfoundation.org"
com.recomdata.dataUpload.etl.dir="/vol1/transmart/ETL/"
com.recomdata.dataUpload.stageScript="run_analysis_stage"

// Folder Manager
com.recomdata.FmFolderService.importDirectory = '/vol1/transmart/import'
com.recomdata.FmFolderService.filestoreDirectory = '/vol1/transmart/filestore'
//Mail Configuration

grails.mail.host = "mailhub.transmartFoundation.org"
grails.mail.port = 25
grails.mail.props = ["mail.smtp.auth":"false", "mail.smtp.starttls.enable":"true"]
// grails.mail.props = ["mail.smtp.auth":"false",
// "mail.smtp.socketFactory.port":"25",
// "mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
// "mail.smtp.socketFactory.fallback":"false"]
grails.mail.default.from = "DL-tranSMART_Support@transmartfoundation.org"
grails.mail.attachments.dir = "/home/transmart/export"

//GWAS Related Configuration:
com.recomdata.dataUpload.appTitle="Upload data to tranSMART (GPL)"
com.recomdata.dataUpload.templates.dir="/home/transmart/templates/"
com.recomdata.dataUpload.uploads.dir="/vol1/transmart/uploads/"
com.recomdata.dataUpload.adminEmail="mailto:DL-tranSMART_Support@transmartfoundation.org"
com.recomdata.dataUpload.etl.dir="/vol1/transmart/ETL/"
com.recomdata.dataUpload.stageScript="run_analysis_stage"

// Folder Manager
com.recomdata.FmFolderService.importDirectory = '/vol1/transmart/import'
com.recomdata.FmFolderService.filestoreDirectory = '/vol1/transmart/filestore'
//Mail Configuration

grails.mail.host = "mailhub.transmartFoundation.org"
grails.mail.port = 25
grails.mail.props = ["mail.smtp.auth":"false", "mail.smtp.starttls.enable":"true"]
// grails.mail.props = ["mail.smtp.auth":"false",
// "mail.smtp.socketFactory.port":"25",
// "mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
// "mail.smtp.socketFactory.fallback":"false"]
grails.mail.default.from = "DL-tranSMART_Support@transmartfoundation.org"
grails.mail.attachments.dir = "/home/transmart/export"


Update run_analysis_stage, load_analysis_stage.sh, move_analysis_to_biomart.sh, nightly_processing.sh to point to the respective directories of the scripts.
ADD FMAPP configuration to kettle.properties:

FMAPP_DB_SERVER=dbserver.com
FMAPP_DB_PORT=1524
FMAPP_DB_USER=fmapp
FMAPP_DB_PWD=fmapp

_____________________________

Update ARCHIVE_DIR variable in Kettle-ETL/load_analysis_from_lz_to_staging.kjb

