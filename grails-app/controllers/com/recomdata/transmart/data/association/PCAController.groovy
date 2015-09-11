package com.recomdata.transmart.data.association

class PCAController {

    def RModulesOutputRenderService

    def pcaOut = {
        //This will be the array of image links.
        def ArrayList<String> imageLinks = new ArrayList<String>()

        //This will be the array of text file locations.
        List<File> txtFiles = []

        //Grab the job ID from the query string.
        String jobName = params.jobName

        //Gather the image links.
        RModulesOutputRenderService.initializeAttributes(jobName, "PCA", imageLinks)

        String tempDirectory = RModulesOutputRenderService.tempDirectory

        //Traverse the temporary directory for the generated image files.
        def tempDirectoryFile = new File(tempDirectory)

        Map<Integer, File> componentsFileMap = constructComponentFileMap(tempDirectoryFile)

        String geneListTable = createGeneListTable(componentsFileMap)

        String summaryTable = RModulesOutputRenderService
                .fileParseLoop(tempDirectoryFile, /.*COMPONENTS_SUMMARY.*\.TXT/, /.*COMPONENTS_SUMMARY(.*)\.TXT/, parseComponentsSummaryStr)

        render(template: "/plugin/pca_out",
                model: [imageLocations: imageLinks, zipLink: RModulesOutputRenderService.zipLink, summaryTable: summaryTable, geneListTable: geneListTable],
                contextPath: pluginContextPath)

    }

    /**
     *
     * @param tempDirectoryFile - directory which contains components text files
     * @return map with component number as key and file as value
     */
    private Map<Integer, File> constructComponentFileMap(File tempDirectoryFile) {
        def fileNamePattern = ~/^GENELIST(?<component>[0-9]+)\.TXT$/

        tempDirectoryFile.listFiles().toList().collectEntries {file ->
            def fileNameMatcher = file.name =~ fileNamePattern
            fileNameMatcher ?
                    [(fileNameMatcher.group('component').toInteger()): file]
                    : [:]
        }.sort()
    }

    /**
     *
     * @param componentsFileMap - components map to render
     * @return String with html representation of components
     */
    private String createGeneListTable(Map<Integer, File> componentsFileMap) {
        def geneListTableHtml = new StringBuffer()
        geneListTableHtml.append '<table><tr>'
        componentsFileMap.eachWithIndex {int component, File file, int ord ->
            if (ord.mod(4) == 0) {
                geneListTableHtml.append '</tr><tr><td>&nbsp;</td></tr><tr>'
            }
            geneListTableHtml.append parseGeneList(file.text, componentsFileMap.size() > 1 ? "$component" : '')
        }
        geneListTableHtml.append '</tr></table><br /><br />'
        geneListTableHtml.toString()
    }

    def parseComponentsSummaryStr = {

        inStr ->

            //These are the buffers we store the HTML text in.
            StringBuffer buf = new StringBuffer();

            boolean firstLine = true

            def resultsItems = [:]

            //Start the table and add headers.
            buf.append("<table class='AnalysisResults'>")
            buf.append("<tr><th>Primary Component</th><th>Eigen Value</th><th>Percent Variance</th></tr>")

            //Iterate over each line of the summary file.
            inStr.eachLine {

                //Every line but the first in the file gets written to the table.
                if (!firstLine) {
                    //Split the current line.
                    String[] resultArray = it.split();

                    //Add the line from the text file to the html table.
                    buf.append("<tr><td>${resultArray[0]}</td><td>${resultArray[1]}</td><td>${resultArray[2]}</td></tr>")
                }

                firstLine = false
            }

            //Close the table
            buf.append("</table><br /><br />")

            return buf.toString()
    }

    def parseGeneList = {

        inStr, currentComponent ->

            boolean firstLine = true

            //These are the buffers we store the HTML text in.
            StringBuffer buf = new StringBuffer();

            buf.append("<td valign='top'><table class='AnalysisResults'><tr><th colspan='2'>Component ${currentComponent}</th></tr>")

            inStr.eachLine {

                //Split the current line.
                String[] resultArray = it.split("\t");

                //Add the line from the text file to the html table.
                buf.append("<tr><td>${resultArray[0]}</td><td>${resultArray[1]}</td></tr>")

                firstLine = false

            }


            buf.append("</table></td>")

            return buf.toString()

    }


}
