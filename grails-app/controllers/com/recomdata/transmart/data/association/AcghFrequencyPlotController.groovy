package com.recomdata.transmart.data.association



class AcghFrequencyPlotController {

    def RModulesOutputRenderService;
    def grailsApplication;

    private getConfig() {
        grailsApplication.config.RModules
    }

    def acghFrequencyPlotOutput =
    {
        def jobTypeName = "acghFrequencyPlot"
        def imageLinks = new ArrayList<String>()

        RModulesOutputRenderService.initializeAttributes(params.jobName, jobTypeName, imageLinks)

        render (template: "/plugin/acghFrequencyPlot_out", model: [zipLink: RModulesOutputRenderService.zipLink, imageLinks: imageLinks])
    }
    /**
     * This function will return the image path
     */
    def imagePath = {
        def imagePath = "${RModulesOutputRenderService.relativeImageURL}${params.jobName}/frequency-plot.png"
        render imagePath
    }

}
