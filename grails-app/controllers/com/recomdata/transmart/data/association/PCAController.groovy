/*************************************************************************   
 * Copyright 2008-2012 Janssen Research & Development, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************/

package com.recomdata.transmart.data.association

class PCAController {

    def RModulesOutputRenderService

    def pcaOut =
        {
            //This will be the array of image links.
            def ArrayList<String> imageLinks = new ArrayList<String>()

            //This will be the array of text file locations.
            List<File> txtFiles = []

            //Grab the job ID from the query string.
            String jobName = params.jobName

            //Gather the image links.
            RModulesOutputRenderService.initializeAttributes(jobName, "PCA", imageLinks)

            String tempDirectory = RModulesOutputRenderService.tempDirectory

            //Traverse the temporary directory for the LinearRegression files.
            def tempDirectoryFile = new File(tempDirectory)

            String summaryTable = RModulesOutputRenderService.fileParseLoop(tempDirectoryFile, /.*COMPONENTS_SUMMARY.*\.TXT/, /.*COMPONENTS_SUMMARY(.*)\.TXT/, parseComponentsSummaryStr)

            def fileNamePattern = ~/^GENELIST(?<component>[0-9]+)\.TXT$/

            Map<Integer, File> componentsFileMap =
                tempDirectoryFile.listFiles().toList().collectEntries {file ->
                    def fileNameMatcher = file.name =~ fileNamePattern
                    fileNameMatcher ?
                        [(fileNameMatcher.group('component').toInteger()): file]
                    : [:]
                }.sort()

            String geneListTable = '<table><tr>'
            componentsFileMap.eachWithIndex {int component, File file, int ord ->
                if (ord.mod(4) == 0) {
                    geneListTable += '</tr><tr><td>&nbsp;</td></tr><tr>'
                }
                geneListTable += parseGeneList(file.text, componentsFileMap.size() > 1 ? "$component" : '')
            }
            geneListTable += '</tr></table><br /><br />'

            render(template: "/plugin/pca_out", model: [imageLocations: imageLinks, zipLink: RModulesOutputRenderService.zipLink, summaryTable: summaryTable, geneListTable: geneListTable], contextPath: pluginContextPath)

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
                String[] resultArray = it.split();

                //Add the line from the text file to the html table.
                buf.append("<tr><td>${resultArray[0]}</td><td>${resultArray[1]}</td></tr>")

                firstLine = false

            }


            buf.append("</table></td>")

            return buf.toString()

    }


}
