package com.recomdata.transmart.data.association

/*************************************************************************
 * tranSMART - translational medicine data mart
 *
 * Copyright 2008-2012 Janssen Research & Development, LLC.
 *
 * This product includes software developed at Janssen Research & Development, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software  * Foundation, either version 3 of the License, or (at your option) any later version, along with the following terms:
 * 1.	You may convey a work based on this program in accordance with section 5, provided that you retain the above notices.
 * 2.	You may convey verbatim copies of this program code as you receive it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS	* FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 ******************************************************************/
import grails.converters.JSON
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.transmartproject.utils.FileUtils

class SurvivalAnalysisResultController {

    def config = ConfigurationHolder.config;
    String temporaryImageFolder = config.RModules.temporaryImageFolder
    String tempFolderDirectory = config.RModules.tempFolderDirectory
    String imageURL = config.RModules.imageURL
    final def DEFAULT_FIELDS = ['chromosome', 'cytoband', 'start', 'end', 'pvalue', 'fdr'] as Set
    final Set DEFAULT_NUMBER_FIELDS = ['start', 'end', 'pvalue', 'fdr'] as Set

    def list = {
        response.contentType = 'text/json'
        if (!(params?.jobName ==~ /(?i)[-a-z0-9]+/)) {
            render new JSON([error: 'jobName parameter is required. It should contains just alphanumeric characters and dashes.'])
            return
        }
        def file = new File("${tempFolderDirectory}", "${params.jobName}/workingDirectory/survival-test.txt")
        if (file.exists()) {
            def fields = params.fields?.split('\\s*,\\s*') as Set ?: DEFAULT_FIELDS

            def obj = FileUtils.parseTable(file,
                    start: params.int('start'),
                    limit: params.int('limit'),
                    fields: fields,
                    sort: params.sort,
                    dir: params.dir,
                    numberFields: DEFAULT_NUMBER_FIELDS,
                    separator: '\t')

            def json = new JSON(obj)
            json.prettyPrint = false
            render json
        } else {
            response.status = 404
            render '[]'
        }
    }

    def image = {

        def imageFile = new File("${temporaryImageFolder}", "${params.jobName}/survival_${params.chromosome}_${params.start}_${params.end}_${params.type ?: '1'}.png")
        if (imageFile.exists()) {
            response.setHeader("Content-disposition", "attachment;filename=${imageFile.getName()}")
            response.contentType = 'image/png'
            response.outputStream << imageFile.getBytes()
            response.outputStream.flush()
        } else {
            response.status = 404
        }
    }

    /**
     * This function will return the image path
     */
    def imagePath = {
        def imagePath = "${imageURL}${params.jobName}/${params.jobType}_${params.chromosome}_${params.start}_${params.end}.png"
        render imagePath
    }

    /**
     * This function returns survival acgh analysis result in zipped file
     */
    def zipFile = {
        def zipFile = new File("${temporaryImageFolder}", "${params.jobName}/zippedData.zip")
        if (zipFile.exists()) {
            response.setHeader("Content-disposition", "attachment;filename=${zipFile.getName()}")
            response.contentType = 'application/octet-stream'
            response.outputStream << zipFile.getBytes()
            response.outputStream.flush()
        } else {
            response.status = 404
        }
    }

    /**
     * This function will return the zipped file path
     */
    def zipFilePath = {
        def zipFilePath = "${imageURL}${params.jobName}/zippedData.zip"
        render zipFilePath
    }

}
