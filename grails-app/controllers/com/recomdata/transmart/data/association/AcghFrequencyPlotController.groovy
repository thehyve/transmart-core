package com.recomdata.transmart.data.association



class ACGHFrequencyPlotController {

    def RModulesOutputRenderService;
    def grailsApplication;

    private getConfig() {
        grailsApplication.config.RModules
    }

    def ACGHFrequencyPlotOutput =
        {
            def jobTypeName = "ACGHFrequencyPlot"
            def imageLinks = new ArrayList<String>()

            RModulesOutputRenderService.initializeAttributes(params.jobName, jobTypeName, imageLinks)

            render (template: "/plugin/ACGHFrequencyPlot_out", model: [zipLink: RModulesOutputRenderService.zipLink, imageLinks: imageLinks])
        }
    /**
     * This function will return the image path
     */
    def imagePath = {
        def imagePath = "${config.imageURL}${params.jobName}/frequency-plot.png"
        render imagePath
    }

    /**
     * This function returns survival acgh analysis result in zipped file
     */
    def zipFile = {
        def zipFile = new File("${config.tempFolderDirectory}", "${params.jobName}/zippedData.zip")
        if(zipFile.exists()) {
            response.setHeader("Content-disposition", "attachment;filename=${zipFile.getName()}")
            response.contentType  = 'application/octet-stream'
            response.outputStream << zipFile.getBytes()
            response.outputStream.flush()
        } else {
            response.status = 404
        }
    }
}
