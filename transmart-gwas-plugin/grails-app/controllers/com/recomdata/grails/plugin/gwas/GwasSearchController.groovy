package com.recomdata.grails.plugin.gwas

import au.com.bytecode.opencsv.CSVWriter
import com.recomdata.transmart.domain.searchapp.FormLayout
import grails.converters.JSON
import org.grails.web.json.JSONObject;
import org.grails.web.mapping.LinkGenerator
import org.transmart.biomart.BioAssayAnalysis
import org.transmart.biomart.Experiment
import org.transmart.searchapp.*
import grails.util.Holders

import java.lang.reflect.UndeclaredThrowableException
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static java.util.UUID.randomUUID

class GwasSearchController {

    def regionSearchService
	def gwasWebService
    def RModulesFileWritingService
    def RModulesJobProcessingService
    def RModulesOutputRenderService
    def springSecurityService
    def gwasSearchService
    def sendFileService

    /**
     * Renders a UI for selecting regions by gene/RSID or chromosome.
     */
    def getRegionFilter = {
        render(template:'/GWAS/regionFilter', model: [ranges:['both':'+/-','plus':'+','minus':'-']], plugin: "transmartGwas")
    }

    def getEqtlTranscriptGeneFilter = {
        render(template:'/GWAS/eqtlTranscriptGeneFilter', plugin: "transmartGwas")
    }

    def webStartPlotter = {

        def codebase = grailsApplication.config.com.recomdata.rwg.webstart.codebase
        def href = grailsApplication.config.com.recomdata.rwg.webstart.href
        def jar = grailsApplication.config.com.recomdata.rwg.webstart.jar
        def mainClass = grailsApplication.config.com.recomdata.rwg.webstart.mainClass
        def gInstance = "-services="+grailsApplication.config.com.recomdata.rwg.webstart.gwavaInstance
		def serverUrl = grailsApplication.config.com.recomdata.rwg.webstart.transmart.url
        def analysisIds = params.analysisIds
        def geneSource = params.geneSource
        def snpSource = params.snpSource
        def pvalueCutoff = params.pvalueCutoff
        def searchRegions = getWebserviceCriteria(session['solrSearchFilter'])
		def user=springSecurityService.getPrincipal().username
        def regionStrings = []
        for (region in searchRegions) {
            regionStrings += region[0] + "," + region[1]
        }
		
		def sessionUserMap = new HashMap<String, String>()
		sessionUserMap = servletContext['gwasSessionUserMap']
		
		if (sessionUserMap == null){
			sessionUserMap = new HashMap<String, String>()
		}
		sessionUserMap.put(session.getId(), user)
		servletContext['gwasSessionUserMap'] = sessionUserMap
		
        def regions = regionStrings.join(";")
        //Set defaults - JNLP does not take blank arguments
        if (!regions) { regions = "0,0" }
        if (!pvalueCutoff) { pvalueCutoff = 0 }

        def responseText = """<?xml version="1.0" encoding="utf-8"?>
							<jnlp
							  spec="1.0+"
							  codebase="${codebase}">
							  <information>
							    <title>GWAVA Gene Wide Association Visual Analyzer with search set</title>
							    <vendor>Pfizer Inc</vendor>
							    <homepage href="./index.html"/>
							    <description>Tool for Manhattan plot visualization of GWAS data.</description>
							    <description kind="short">GWAVA gene wide association visual analysis</description>
							    <shortcut>
							      <desktop/>
							      <menu submenu="GWAVA Transmart"/>
							    </shortcut>
							    <icon href="./images/guava_16.jpg"/>
							    <icon href="./images/guava_24.jpg"/>
							    <icon href="./images/guava_48.jpg"/>
							    <icon kind="splash" href="./images/gwava_splash2.jpg"/>
							    <offline-allowed/>
							  </information>
							  <security>
							      <all-permissions/>
							  </security>
							  <update check="always" policy="always"/>
							  <resources>
							    <j2se version="1.6+" java-vm-args="-Xmx800m"/>

							    <jar href="./lib/commons-beanutils-1.8.3.jar"/>
							    <jar href="./lib/commons-beanutils-bean-collections-1.8.3.jar"/>
							    <jar href="./lib/commons-beanutils-core-1.8.3.jar"/>
							    <jar href="./lib/commons-codec-1.6.jar"/>
							    <jar href="./lib/commons-digester3-3.2.jar"/>
							    <jar href="./lib/commons-lang3-3.1.jar"/>
							    <jar href="./lib/commons-logging-1.1.1.jar"/>
							    <jar href="./lib/httpclient-4.0.jar"/>
							    <jar href="./lib/httpcore-4.2.1.jar"/>
							    <jar href="./lib/jersey-client-1.4.jar"/>
							    <jar href="./lib/jersey-core-1.4.jar"/>
							    <jar href="./lib/jgoodies-common-1.3.1.jar"/>
							    <jar href="./lib/jgoodies-looks-2.5.1.jar"/>
							    <jar href="./lib/log4j-1.2.17.jar"/>
							    <jar href="${jar}"/>

							    <property name="jsessionid" value='""" + session.getId() + """'/>
                                <property name="serviceHost" value='""" + request.getServerName() + """'/>
                                <property name="sun.java2d.noddraw" value="true"/>
							  </resources>
							  <application-desc main-class="com.pfizer.mrbt.genomics.Driver">
								<argument>""" + gInstance + """</argument>
								<argument>""" + analysisIds + """</argument>
								<argument>""" + regions + """</argument>
								<argument>0</argument>
								<argument>""" + snpSource + """</argument>
								<argument>""" + pvalueCutoff + """</argument>
								<argument>""" + serverUrl + """</argument>
                                <argument>""" + session.getId() + """</argument>
							  </application-desc>

							</jnlp>
		"""

        render(text:responseText,contentType:"application/x-java-jnlp-file")
        //render(text:responseText,contentType:"text/html")
    }

    def getWebserviceCriteria(solrSearch) {
        def genes = []

        for (s in solrSearch) {
            if (s.startsWith("REGION")) {
                //Cut off REGION:, split by pipe and interpret chromosomes and genes
                s = s.substring(7)
                def regionparams = s.split("\\|")
                for (r in regionparams) {
                    //Chromosome
                    if (r.startsWith("CHROMOSOME")) {
                        //Do nothing for now
                    }
                    //Gene
                    else {
                        def region = r.split("\\^")
                        def geneId = region[1] as long
                        def direction = region[2]
                        def range = region[3] as long
                        def ver = region[4]
                        def searchKeyword = SearchKeyword.get(geneId)
                        def limits
                        if (searchKeyword.dataCategory.equals("GENE")) {
                            genes.push([searchKeyword.keyword, range])
                        }
                        else if (searchKeyword.dataCategory.equals("SNP")) {
                            //Get the genes associated with this SNP
                            def snpGenes = regionSearchService.getGenesForSnp(searchKeyword.keyword)
                            //Push each gene and the radius
                            for (snpGene in snpGenes) {
                                genes.push([snpGene, range])
                            }
                        }

                    }
                }
            }
            else if (s.startsWith("GENESIG")) {
                //Expand regions to genes and get their limits
                s = s.substring(8)
                def sigIds = s.split("\\|")
                for (sigId in sigIds) {
                    def sigSearchKeyword = SearchKeyword.get(sigId as long)
                    def sigItems = GeneSignatureItem.createCriteria().list() {
                        eq('geneSignature', GeneSignature.get(sigSearchKeyword.bioDataId))
                        like('bioDataUniqueId', 'GENE%')
                    }
                    for (sigItem in sigItems) {
                        def searchGene = SearchKeyword.findByUniqueId(sigItem.bioDataUniqueId)
                        def geneId = searchGene.id
                        genes.push([searchGene.keyword, 0]);
                    }
                }
            }
            else if (s.startsWith("GENE")) {
                s = s.substring(5)
                def geneIds = s.split("\\|")
                for (geneString in geneIds) {
                    def geneId = geneString as long
                    def searchKeyword = SearchKeyword.get(geneId)
                    genes.push([searchKeyword.keyword, 0])
                }
            }
            else if (s.startsWith("SNP")) {
                //If plain SNPs, as above (default to HG19)
                s = s.substring(4)
                def rsIds = s.split("\\|")
                for (rsId in rsIds) {
                    //Get the genes associated with this SNP
                    def searchKeyword = SearchKeyword.get(rsId as long)
                    def snpGenes = regionSearchService.getGenesForSnp(searchKeyword.keyword)
                    //Push each gene and the radius
                    for (snpGene in snpGenes) {
                        genes.push([snpGene, 0])
                    }
                }
            }
        }

        return genes
    }

    def getRegionSearchResults(Long max, Long offset, Double cutoff, String sortField, String order, String search, analysisIds) throws Exception {

        //Get list of REGION restrictions from session and translate to regions
        def regions = getSearchRegions(session['solrSearchFilter'])
        def geneNames = getGeneNames(session['solrSearchFilter'])
        if (getSearchCutoff(session['solrSearchFilter'])){
            cutoff = getSearchCutoff(session['solrSearchFilter'])
        }
        def transcriptGeneNames = getTranscriptGeneNames(session['solrSearchFilter'])
        //Find out if we're querying for EQTL, GWAS, or both
        def hasGwas = BioAssayAnalysis.createCriteria().list([max: 1]) {
            or {
                eq('assayDataType', 'GWAS')
                eq('assayDataType', 'Metabolic GWAS')
                eq('assayDataType','GWAS Fail')
            }
            'in'('id', analysisIds)
        }

        def hasEqtl = BioAssayAnalysis.createCriteria().list([max: 1]) {
            eq('assayDataType', 'EQTL')
            'in'('id', analysisIds)
        }

        def gwasResult
        def eqtlResult

        if (hasGwas) {
            gwasResult = runRegionQuery(analysisIds, regions, max, offset, cutoff, sortField, order, search, "gwas", geneNames, transcriptGeneNames)
        }
        if (hasEqtl) {
            eqtlResult = runRegionQuery(analysisIds, regions, max, offset, cutoff, sortField, order, search, "eqtl", geneNames, transcriptGeneNames)
        }

        return [gwasResults: gwasResult, eqtlResults: eqtlResult]
    }


    def runRegionQuery(analysisIds, regions, max, offset, cutoff, sortField, order, search, type, geneNames, transcriptGeneNames) throws Exception {

        //This will hold the index lookups for deciphering the large text meta-data field.
        def indexMap = [:]

        //Set a flag to record that the list was filtered by region
        def wasRegionFiltered = regions ? true : false

        def queryResult
        def analysisData = []
        def totalCount

        def columnNames = []

        def wasShortcut = false
        if (!regions && !geneNames && !transcriptGeneNames && analysisIds.size() == 1 && sortField.equals('null') && !cutoff && !search && max > 0) {
            println("Triggering shortcut query")
            wasShortcut = true
            //If displaying no regions and only one analysis, run the alternative query and pull back the rows for the limits
            def analysis = BioAssayAnalysis.get(analysisIds[0])
            def quickAnalysisData = regionSearchService.getQuickAnalysisDataByName(analysis.name, type)
            for (int i = offset; i < (max+offset); i++) {
                analysisData.push(quickAnalysisData.results[i]);
            }
            totalCount = analysis.dataCount
            println("Got results in a batch of " + analysisData.size())
        }
        else {
            //Otherwise, run the query and recache the returned data
			if (sortField.equals('null')) {sortField = 'data.log_p_value'; order='desc';}
            queryResult = regionSearchService.getAnalysisData(analysisIds, regions, max, offset, cutoff, sortField, order, search, type, geneNames, transcriptGeneNames, true)
            analysisData = queryResult.results
            totalCount = queryResult.total

            //Cache if this isn't for an export
//			if (max > 0) {
//				session['cachedAnalysisData'] = analysisData
//				session['cachedCount'] = totalCount
//			}
        }


        def analysisIndexData
        if (type.equals("eqtl")) {
            analysisIndexData = gwasSearchService.getEqtlIndexData()
        }
        else {
            analysisIndexData = gwasSearchService.getGwasIndexData()
        }
        def returnedAnalysisData = []

        //These columns aren't dynamic and should always be included. Might be a better way to do this than just dropping it here.
        columnNames.add(["sTitle":"Analysis", "sortField":"baa.analysis_name"])
        columnNames.add(["sTitle":"Probe ID", "sortField":"data.rs_id"])
        columnNames.add(["sTitle":"p-value", "sortField":"data.p_value"])
        columnNames.add(["sTitle":"-log 10 p-value", "sortField":"data.log_p_value"])
        columnNames.add(["sTitle":"RS Gene", "sortField":"gmap.gene_name"])
        columnNames.add(["sTitle":"Chromosome", "sortField":"info.chrom"])
        columnNames.add(["sTitle":"Position", "sortField":"info.pos"])
        columnNames.add(["sTitle":"Exon/Intron", "sortField":"info.exon_intron"])
        columnNames.add(["sTitle":"Recombination Rate", "sortField":"info.recombination_rate"])
        columnNames.add(["sTitle":"Regulome Score", "sortField":"info.regulome_score"])

        if (type.equals("eqtl")) {
            columnNames.add(["sTitle":"Transcript Gene", "sortField":"data.gene"])
        }

        analysisIndexData.each()
                {
                    //Put the index information into a map so we can look it up later.
                    indexMap[it.field_idx] = it.display_idx

                    //We need to take the data from the index table and extract the list of column names.
                    columnNames.add(["sTitle":it.field_name])
                }

        println("About to process results")

        //The returned data needs to have the large text field broken out by delimiter.
        analysisData.each()
                {
                    if (it != null) {
                        //This temporary list is used so that we return a list of lists.
                        def temporaryList = []

                        //The third element is our large text field. Split it into an array, leaving trailing empties.
                        def largeTextField = it[3]?.split(";", -1)

                        //This will be the array that is reordered according to the meta-data index table.
                        //String[] newLargeTextField = new String[largeTextField.size()]
                        String[] newLargeTextField = new String[indexMap.size()]
                        def counter=0;
                        //Loop over the elements in the index map.
                        indexMap.each()
                                {
                                    //Reorder the array based on the index table.
                                    //if (it.key-1<newLargeTextField.size())
                                    if (it.key-1<largeTextField?.size())
                                    {
                                        //log.warn("Key: "+it.key+ "size "+newLargeTextField.size());
                                        newLargeTextField[it.value-1] = largeTextField[it.key-1]
                                        counter++;
                                    }
                                    else
                                    {
                                        //log.warn("Else clause Key: "+it.key+ "size "+newLargeTextField.size());
                                        newLargeTextField[counter]="";
                                        counter++;
                                    }
                                }

                        //Swap around the data types for easy array addition.
                        def finalFields = new ArrayList(Arrays.asList(newLargeTextField));

                        //Add the non-dynamic meta data fields to the returned data.
                        temporaryList.add(it[4])
                        temporaryList.add(it[0])
                        temporaryList.add(it[1])
                        temporaryList.add(it[2])
                        temporaryList.add(it[5])
                        temporaryList.add(it[6])
                        temporaryList.add(it[7])
                        temporaryList.add(it[8])
                        temporaryList.add(it[9])
                        temporaryList.add(it[10])
                        if (type.equals("eqtl")) {
                            temporaryList.add(it[11])
                        }

                        //Add the dynamic fields to the returned data.
                        temporaryList+=finalFields

                        returnedAnalysisData.add(temporaryList)
                    }
                }

        log.warn("Results processed")
        println("Results processed OK")
        return [analysisData: returnedAnalysisData, columnNames: columnNames, max: max, offset: offset, cutoff: cutoff, totalCount: totalCount, wasRegionFiltered: wasRegionFiltered, wasShortcut: wasShortcut]

    }

    private String getCachedImageDir() {
        grailsApplication.config.com.recomdata.rwg.qqplots.cacheImages
    }

    private File cachedImagePathFor(Long analysisId) {
        new File(new File(cachedImageDir, analysisId as String), 'QQPlot.png')
    }

    private String imageUrlFor(Long analysisId) {
        grailsLinkGenerator.link(
                controller: 'gwasSearch',
                action: 'downloadQQPlotImage',
                absolute: true,
                params: [analysisId: analysisId])
    }

    def downloadQQPlotImage() {
        // Should probably check access

        def analysisId = params.getLong('analysisId')
        if (!analysisId) {
            log.warn "Request without analysisId"
            render status: 404
            return
        }

        def targetFile = cachedImagePathFor(analysisId)

        if (!targetFile.isFile()) {
            log.warn "Request for $targetFile, but such file does not exist"
            render status: 404
            return
        }

        sendFileService.sendFile servletContext, request, response, targetFile
    }

    def getQQPlotImage = {

        def returnJSON = [:]

        try {
            //We need to determine the data type of this analysis so we know where to pull the data from.
            def currentAnalysis = BioAssayAnalysis.get(params.analysisId)
			String explodedDeplDir = servletContext.getRealPath("/");
			String tempImageFolder = grailsApplication.config.com.recomdata.plugins.tempFolderDirectory

            //get rdc-modules plugin info
			String pluginDir =  grailsApplication.config.RModules.pluginScriptDirectory;

			File cachedQqPlotFile = cachedImagePathFor(params.getLong('analysisId'))
			
			// use QQPlots cached images if they are available. QQPlots takes >10 minutes to run and only needs to be generated once per analysis.
			if (cachedQqPlotFile.exists()) {
				returnJSON['imageURL'] = imageUrlFor(params.getLong('analysisId'))
				render returnJSON as JSON;
				return;
			}
			
            def pvalueCutoff = params.double('pvalueCutoff')
            def search = params.search

            if (!pvalueCutoff) {pvalueCutoff = 0}
            if (!search) {search = ""}

            //Throw an error if we don't find the analysis for some reason.
            if(!currentAnalysis)
            {
                throw new Exception("Analysis not found.")
            }

            //This will hold the index lookups for deciphering the large text meta-data field.
            def indexMap = [:]

            //Get the GWAS Data. Call a different class based on the data type.
            def analysisData

            //Get the data from the index table for GWAS.
            def analysisIndexData

            def returnedAnalysisData = []

            //Get list of REGION restrictions from session and translate to regions
            def regions = getSearchRegions(session['solrSearchFilter'])
            def geneNames = getGeneNames(session['solrSearchFilter'])
            def transcriptGeneNames = getTranscriptGeneNames(session['solrSearchFilter'])
            def analysisIds = [currentAnalysis.id]

			
            switch(currentAnalysis.assayDataType)
            {
                case "GWAS" :
				case "GWAS Fail" :
                case "Metabolic GWAS" :
                    analysisData = regionSearchService.getAnalysisData(analysisIds, regions, 0, 0, pvalueCutoff, "null", "asc", search, "gwas", geneNames, transcriptGeneNames, false).results
                    analysisIndexData = gwasSearchService.getGwasIndexData()
                    break;
                case "EQTL" :
                    analysisData = regionSearchService.getAnalysisData(analysisIds, regions, 0, 0, pvalueCutoff, "null", "asc", search, "eqtl", geneNames, transcriptGeneNames, false).results
                    analysisIndexData = gwasSearchService.getEqtlIndexData()
                    break;
                default :
                    throw new Exception("No applicable data type found.")
            }

            analysisIndexData.each()
                    {
                        //Put the index information into a map so we can look it up later. Only add the GOOD_CLUSTERING column.
                        if(it.field_name == "GOOD_CLUSTERING")
                        {
                            indexMap[it.field_idx] = it.display_idx
                        }
                    }

            //Create an entry that represents the headers to print to the file.
            def columnHeaderList = ["PROBEID","pvalue","good_clustering"]
            returnedAnalysisData.add(columnHeaderList)

            //The returned data needs to have the large text field broken out by delimiter.
            analysisData.each()
                    {
                        //This temporary list is used so that we return a list of lists.
                        def temporaryList = []

                        //This will be used to fill in the data array.
                        def indexCount = 0;

                        //The third element is our large text field. Split it into an array.
                        def largeTextField = it[3]?.split(";", -1)

                        //This will be the array that is reordered according to the meta-data index table.
                        String[] newLargeTextField = new String[indexMap.size()]

                        //Loop over the elements in the index map.
                        indexMap.each()
                                {
                                    //Reorder the array based on the index table.
                                    newLargeTextField[indexCount] = largeTextField[it.key-1]

                                    indexCount++;
                                }

                        //Swap around the data types for easy array addition.
                        def finalFields = new ArrayList(Arrays.asList(newLargeTextField));

                        //Add the non-dynamic meta data fields to the returned data.
                        temporaryList.add(it[0])
                        temporaryList.add(it[1])

                        //Add the dynamic fields to the returned data.
                        temporaryList+=finalFields

                        returnedAnalysisData.add(temporaryList)
                    }
			
            println "QQPlot row count = " + returnedAnalysisData.size()
            //		for (int i = 0; i < returnedAnalysisData.size() && i < 10; i++) {
            //			println returnedAnalysisData[i]
            //		}

            //Get a unique key for the image file.
            def uniqueId = randomUUID() as String

            //Create a unique name using the id.
            def uniqueName = "QQPlot-" + uniqueId

            //Create the temporary directories for processing the image.
            def currentTempDirectory = gwasWebService.createTemporaryDirectory(uniqueName)

            def currentWorkingDirectory =  currentTempDirectory + File.separator + "workingDirectory" + File.separator

            //Write the data file for generating the image.
            def currentDataFile = gwasWebService.writeDataFile(currentWorkingDirectory, returnedAnalysisData,"QQPlot.txt")

            //Run the R script to generate the image file.
			RModulesJobProcessingService.runRScript(currentWorkingDirectory,"/QQ/QQPlot.R","create.qq.plot('QQPlot.txt')", pluginDir)

            //Verify the image file exists.
            def imagePath = currentWorkingDirectory + File.separator + "QQPlot.png"

            if(!new File(imagePath))
            {
                throw new Exception("Image file creation failed!")
            }
            else
            {
				FileUtils.copyFile(new File(imagePath), cachedQqPlotFile);
				returnJSON['imageURL'] = imageUrlFor(currentAnalysis.id)
				render returnJSON as JSON;
				return;
            }
        }
        catch (Exception e) {
            response.status = 500
            renderException(e)
        }
    }

    //Retrieve the results for the search filter. This is used to populate the result grids on the search page.
    def getAnalysisResults = {

        //TODO Combine this and the table method, they're now near-identical
        def paramMap = params;
        def max = params.long('max')
        def offset = params.long('offset')
        def cutoff = params.double('cutoff')
        if ("".equals(params.cutoff)) {cutoff = 0;} //Special case - cutoff is 0 if blank string
        def sortField = params.sortField
        def order = params.order
        def search = params.search

        def analysisId = params.long('analysisId')
        def export = params.boolean('export')

        def filter = session['filterAnalysis' + analysisId];
        if (filter == null) {
            filter = [:]
        }
        if (max != null) { filter.max = max }
        if (!filter.max || filter.max < 10) {filter.max = 10;}

        if (offset != null) { filter.offset = offset }
        if (!filter.offset || filter.offset < 0) {filter.offset = 0;}

        if (cutoff != null) { filter.cutoff = cutoff }

        if (sortField != null) { filter.sortField = sortField }
        if (!filter.sortField) {filter.sortField = 'null';}

        if (order != null) { filter.order = order }
        if (!filter.order) {filter.order = 'asc';}

        if (search != null) { filter.search = search }

        def analysisIds = []
        analysisIds.push(analysisId)

        session['filterAnalysis' + analysisId] = filter;

        //Override max and offset if we're exporting
        def maxToUse = filter.max
        def offsetToUse = filter.offset
        if (export) {
            maxToUse = 0
            offsetToUse = 0
        }

        def regionSearchResults
        try {
            regionSearchResults = getRegionSearchResults(maxToUse, offsetToUse, filter.cutoff, filter.sortField, filter.order, filter.search, analysisIds)
        }
        catch (Exception e) {
            renderException(e);
            return
        }

        try {
            //regionSearchResults will either contain GWAS or EQTL data. Overwrite the base object with the one that's populated
            if (regionSearchResults.gwasResults) {
                regionSearchResults = regionSearchResults.gwasResults
            }
            else {
                regionSearchResults = regionSearchResults.eqtlResults
            }

            if (!regionSearchResults) {
                render(text: "<p>No data could be found for this analysis type.</p>")
                return
            }

            //Return the data as a GRAILS template or CSV
            if (export) {
                exportResults(regionSearchResults.columnNames, regionSearchResults.analysisData, "analysis" + analysisId + ".csv")
            }
            else {
                render(plugin: "transmartGwas", template: "/GWAS/analysisResults", model: [analysisData: regionSearchResults.analysisData, columnNames: regionSearchResults.columnNames, max: regionSearchResults.max, offset: regionSearchResults.offset, cutoff: filter.cutoff, sortField: filter.sortField, order: filter.order, search: filter.search, totalCount: regionSearchResults.totalCount, wasRegionFiltered: regionSearchResults.wasRegionFiltered, wasShortcut: regionSearchResults.wasShortcut, analysisId: analysisId])
            }
        }
        catch (Exception e) {
            render(status: 500, text: e.getMessage())
        }
    }

    //Retrieve the results for all analyses currently examined.
    def getTableResults = {

        def paramMap = params;
        def max = params.long('max')
        def offset = params.long('offset')
        def cutoff = params.double('cutoff')
        if ("".equals(params.cutoff)) {cutoff = 0;} //Special case - cutoff is 0 if blank string
        def sortField = params.sortField
        def order = params.order
        def search = params.search

        def export = params.boolean('export')

        def filter = session['filterTableView'];
        if (filter == null) {
            filter = [:]
        }

        if (max != null) { filter.max = max }
        if (!filter.max || filter.max < 10) {filter.max = 10;}

        if (offset != null) { filter.offset = offset }
        if (!filter.offset || filter.offset < 0) {filter.offset = 0;}

        if (cutoff != null) { filter.cutoff = cutoff }

        if (sortField != null) { filter.sortField = sortField }
        if (!filter.sortField) {filter.sortField = 'null';}

        if (order != null) { filter.order = order }
        if (!filter.order) {filter.order = 'asc';}

        if (search != null) { filter.search = search }

        def analysisIds = session['solrAnalysisIds']

        if (analysisIds[0] == -1) {
            // in the case that no filter is selected - where we get no a "not a set" indicator from the session
            // which results in an empty set after the intersection with "allowed ids" below
            render(text: "<p>To use the table view, please select one of more filters from the filter browser in the left pane.</p>")
            return
        }

        // following code will limit analysis ids to ones that the user is allowed to access
        def user=AuthUser.findByUsername(springSecurityService.getPrincipal().username)
        def secObjs=getExperimentSecureStudyList()
        def analyses = BioAssayAnalysis.executeQuery("select id, name, etlId from BioAssayAnalysis b order by b.name")
        analyses=analyses.findAll{!secObjs.containsKey(it[2]) || !gwasWebService.getGWASAccess(it[2], user).equals("Locked") }
        analyses=analyses.findAll {analysisIds.contains(it[0])} // get intersection of all analyses id and allowed ids

        def allowedAnalysisIds = [] // will be used to his temporary list

        analyses.each { allowedAnalysisIds.add(it[0])} // fill list with ids from analyses object
        analysisIds = allowedAnalysisIds // replace all analysis ids with intersection ids

        //session['filterTableView'] = filter

/*		if (analysisIds.size() >= 100) {
			render(text: "<p>The table view cannot be used with more than 100 analyses (${analysisIds.size()} analyses in current search results). Narrow down your results by adding filters.</p>")
			return
		}
		else*/
        if (analysisIds.size() == 0) {
            render(text: "<p>No analyses were found for the current filter!</p>")
            return
        }

        //Override max and offset if we're exporting
        def maxToUse = filter.max
        def offsetToUse = filter.offset
        if (export) {
            maxToUse = 0
            offsetToUse = 0
        }

        def regionSearchResults
        try {
            regionSearchResults = getRegionSearchResults(maxToUse, offsetToUse, filter.cutoff, filter.sortField, filter.order, filter.search, analysisIds)
        }
        catch (Exception e) {
            renderException(e);
            return
        }
        //Return the data as a GRAILS template or CSV
        if (export) {
            if (params.type?.equals('GWAS')) {
                exportResults(regionSearchResults.gwasResults.columnNames, regionSearchResults.gwasResults.analysisData, "results.csv")
            }
            else {
                exportResults(regionSearchResults.eqtlResults.columnNames, regionSearchResults.eqtlResults.analysisData, "results.csv")
            }
        }
        else {
            render(plugin: "transmartGwas", template: "/GWAS/gwasAndEqtlResults", model: [results: regionSearchResults, cutoff: filter.cutoff, sortField: filter.sortField, order: filter.order, search: filter.search])
        }
    }

    def getSearchCutoff(solrSearch) {
        def cutoff
        for (s in solrSearch) {
            if (s.startsWith("PVALUE") && s.length() > 6) {
                s = s.substring(7)
                def pvalue = s.split("\\^")
                if (pvalue.size() > 1) {
                    cutoff = pvalue[1]
                }
            }
        }
        if (cutoff) {
            return cutoff.toDouble()
        }
    }

    def getSearchRegions(solrSearch) {
        def regions = []

        for (s in solrSearch) {
            if (s.startsWith("REGION")) {
                //Cut off REGION:, split by pipe and interpret chromosomes and genes
                s = s.substring(7)
                def regionparams = s.split("\\|")
                for (r in regionparams) {
                    //Chromosome
                    if (r.startsWith("CHROMOSOME")) {
                        def region = r.split("\\^")
                        def chrom = region[1]
                        def position = region[3] as long
                        def direction = region[4]
                        def range = region[5] as long
                        def ver = region[6]
                        def low = position
                        def high = position

                        if (direction.equals("plus")) {
                            high = position + range;
                        }
                        else if (direction.equals("minus")) {
                            low = position - range;
                        }
                        else {
                            high = position + range;
                            low = position - range;
                        }

                        regions.push([gene: null, chromosome: chrom, low: low, high: high, ver: ver])
                    }
                    //Gene
                    else {
                        def region = r.split("\\^")
                        def geneId = region[1] as long
                        def direction = region[2]
                        def range = region[3] as long
                        def ver = region[4]
                        def searchKeyword = SearchKeyword.get(geneId)
                        def limits
                        if (searchKeyword.dataCategory.equals("GENE")) {
                            limits = regionSearchService.getGeneLimits(geneId, ver, 0L)
                        }
                        else if (searchKeyword.dataCategory.equals("SNP")) {
                            limits = regionSearchService.getSnpLimits(geneId, ver, 0L)
                        }
                        if (limits) {
                            def low = limits.get('low')
                            def high = limits.get('high')
                            def chrom = limits.get('chrom')

                            if (direction.equals("plus")) {
                                high = high + range;
                            }
                            else if (direction.equals("minus")) {
                                low = low - range;
                            }
                            else {
                                high = high + range;
                                low = low - range;
                            }
                            regions.push([gene: geneId, chromosome: chrom, low: low, high: high, ver: ver])
                        } else {
                            log.error("regionSearchService, called from GwasSearchController.getSearchRegions, returned" +
                                    "a null value for limit; most likely this is from a filter request that will fail" +
                                    "as a consiquence of this error.")
                        }
                    }
                }
            }
            else if (s.startsWith("GENESIG") || s.startsWith("GENELIST")  ) {

                while (s.startsWith("GENELIST")) {
                    s = s.substring(9)
                } 

                while (s.startsWith("GENESIG")) {
                    s = s.substring(8)
                }
                def sigIds = s.split("\\|")
                for (sigId in sigIds) {

                    //def sigSearchKeyword = SearchKeyword.get(sigId as long)
                    def sig = GeneSignature.get(sigId as long) // sigSearchKeyword.bioDataId)
                    def sigItems = GeneSignatureItem.createCriteria().list() {
                        eq('geneSignature', sig)
                        or {
                            like('bioDataUniqueId', 'GENE%')
                            like('bioDataUniqueId', 'SNP%')
                        }
                    }
                    for (sigItem in sigItems) {
                        def searchItem = SearchKeyword.findByUniqueId(sigItem.bioDataUniqueId)
						
							if (searchItem?.dataCategory?.equals('SNP')) {
								def rsId = searchItem.id as long
								if (!rsId) {
									println("No SNP found for identifier:" + sigItem.bioDataUniqueId)
									continue
								}
								def limits = regionSearchService.getSnpLimits(rsId, '19', sig.flankingRegion)
								regions.push([gene: rsId, chromosome: limits.get('chrom'), low: limits.get('low'), high: limits.get('high'), ver: "19"])
							}
							else if (searchItem?.dataCategory?.equals('GENE')) {
								def geneId = searchItem?.id
								def limits = regionSearchService.getGeneLimits(geneId, '19', sig.flankingRegion)
								if (limits!=null)
								{
								regions.push([gene: geneId, chromosome: limits.get('chrom'), low: limits.get('low'), high: limits.get('high'), ver: "19"])
								}
								else
								{
									log.debug("Gene not found deapp:"+geneId)
								}
							}
						

                    }
                }
            }
            else if (s.startsWith("GENE")) {
                //If just plain genes, get the limits and default to HG19 as the version
                s = s.substring(5)
                def geneIds = s.split("\\|")
                for (geneString in geneIds) {
                    def geneSearchItem = SearchKeyword.findByUniqueId(geneString)
                    def geneId = geneSearchItem.id 
                    def limits = regionSearchService.getGeneLimits(geneId, '19', 0L)
                    regions.push([gene: geneId, chromosome: limits.get('chrom'), low: limits.get('low'), high: limits.get('high'), ver: "19"])
                }
            }
            else if (s.startsWith("SNP")) {
                //If plain SNPs, as above (default to HG19)
                s = s.substring(4)
                def rsIds = s.split("\\|")
                for (rsId in rsIds) {
                    //def snpSearchItem = SearchKeyword.findByUniqueId(rsId)
                    //def snpId = snpSearchItem.id 
                    def limits = regionSearchService.getSnpLimits(rsId as long, '19', 0L)
                    regions.push([gene: rsId, chromosome: limits.get('chrom'), low: limits.get('low'), high: limits.get('high'), ver: "19"])
                }
            }
        }

        return regions
    }

    def getGeneNames(solrSearch) {
        def genes = []

        for (s in solrSearch) {
            if (s.startsWith("GENESIG")|| s.startsWith("GENELIST"))  {

                while (s.startsWith("GENELIST")) {
                    s = s.substring(9)
                } 

                while (s.startsWith("GENESIG")) {
                    s = s.substring(8)
                }

                //Expand regions to genes and get their names
                //s = s.substring(8)
                def sigIds = s.split("\\|")
                for (sigId in sigIds) {
                    //def sigSearchKeyword = SearchKeyword.get(sigId as long)
                    def sigItems = GeneSignatureItem.createCriteria().list() {
                        eq('geneSignature', GeneSignature.get(sigId))  //sigSearchKeyword.bioDataId))
                        like('bioDataUniqueId', 'GENE%')
                    }
                    for (sigItem in sigItems) {
                        def searchGene = SearchKeyword.findByUniqueId(sigItem.bioDataUniqueId)
                        def geneId = searchGene?.id
                        if (!geneId) continue; //Signature may contain SNPs or probes
                        def searchKeyword = SearchKeyword.get(geneId)
                        genes.push(searchKeyword.keyword)
                    }
                }
            }
            else if (s.startsWith("GENE")) {
                //If just plain genes, get the names
                s = s.substring(5)
                def geneIds = s.split("\\|")
                for (geneString in geneIds) {
                    //def geneId = geneString as long
                    def geneSearchItem = SearchKeyword.findByUniqueId(geneString)
                    def geneId = geneSearchItem.id
                    def searchKeyword = SearchKeyword.get(geneId)
                    genes.push(searchKeyword.keyword)
                }
            }
        }

        return genes
    }

    def getTranscriptGeneNames(solrSearch) {
        def genes = []

        for (s in solrSearch) {
            if (s.startsWith("TRANSCRIPTGENE")) {
                //If just plain genes, get the names
                s = s.substring(15)
                def geneIds = s.split("\\|")
                for (geneString in geneIds) {
                    genes.push(geneString)
                }
            }
        }

        return genes
    }


    def renderException(e) {
        e.printStackTrace()

        if (e instanceof UndeclaredThrowableException) {
            def ute = (UndeclaredThrowableException) e
            e = ute.getUndeclaredThrowable()
        }

        while (e.getCause() && e.getCause() != e) {
            e = e.getCause()
        }

        def stackTrace = e.getStackTrace()

        render(text: "<div class='errorbox'>tranSMART encountered an error while running this query (" + e.class.getName() + " " + e.getMessage() + "). Please contact an administrator with your search criteria and the information below.</div>")
        render(text: "<pre class='errorstacktrace'>")
        render(text: "<b>Error while retrieving data: " + e.class.getName() + ".</b> Message: " + e.getMessage() + "\n")

        for (el in stackTrace) {
            render(text: "\t" + el.getClassName() + "." + el.getMethodName() + ", line " + el.getLineNumber() + " " + "\n")
        }
        render("</pre>")
    }

    def exportResults(columns, rows, filename) {

        response.setHeader('Content-disposition', 'attachment; filename=' + filename)
        response.contentType = 'text/plain'

        String lineSeparator = System.getProperty('line.separator')
        CSVWriter csv = new CSVWriter(response.writer)
        def headList = []
        for (column in columns) {
            headList.push(column.sTitle)
        }
        String[] head = headList
        csv.writeNext(head)

        for (row in rows) {
            def rowData = []
            for (data in row) {
                rowData.push(data)
            }
            String[] vals = rowData
            csv.writeNext(vals)
        }
        csv.close()
    }

    //Common Method to export analysis data as link or attachment
    def exportAnalysisData(analysisId, dataWriter,cutoff,regions,geneNames,transcriptGeneNames,max) {
        def analysis = BioAssayAnalysis.findById(analysisId, [max: 1])
        def analysisArr = []
        analysisArr.push(analysisId)
        def query
        if (analysis.assayDataType == "GWAS" || analysis.assayDataType == "Metabolic GWAS" || analysis.assayDataType == "GWAS Fail") {
            query=regionSearchService.getAnalysisData(analysisArr, regions, max, 0, cutoff, "data.log_p_value", "desc", null, "gwas", geneNames,transcriptGeneNames,false)
        } else {
            query=regionSearchService.getAnalysisData(analysisArr, regions, max, 0, cutoff, "data.log_p_value", "desc", null, "eqtl", geneNames,transcriptGeneNames,false)
        }
		log.debug("Before the result")
        def dataset = query.results
		log.debug("Should be using probes")
        dataWriter.write "Probe ID\tp-value\t-log10 p-value\tRS Gene\tChromosome\tPosition\tInteronExon\tRecombination Rate\tRegulome Score\n"
        for (row in dataset) {
            def rowData = []
            for (data in row) {
                rowData.push(data)
            }
            String[] vals = rowData
            //println vals
            for (int i = 0; i < vals.size(); i++) {
                if ((i < 3) || (i > 4)) {
                    if (vals[i] != null) {
                        dataWriter.write vals[i] + "\t"
                    } else {
                        dataWriter.write "\t"
                    }
                }
            }
            dataWriter.write "\n"
        }

        dataWriter.close()
    }
	def getExperimentSecureStudyList(){  //ZHANH101
		
		StringBuilder s = new StringBuilder();
		s.append("SELECT so.bioDataUniqueId, so.bioDataId FROM SecureObject so Where so.dataType='Experiment'")
		def t=[:];
		//return access levels for the children of this path that have them
		def results = SecureObject.executeQuery(s.toString());
		for (row in results){
			def token = row[0];
			def dataid = row[1];
			token=token.replaceFirst("EXP:","")
			log.info(token+":"+dataid);
			t.put(token,dataid);
		}
		return t;
	}
    def exportAnalysis = {

        def paramMap = params;
        def isLink = params.isLink
        def cutoff = params.double('cutoff')
        if (getSearchCutoff(session['solrSearchFilter'])){
            cutoff = getSearchCutoff(session['solrSearchFilter'])
        }
        def regions = getSearchRegions(session['solrSearchFilter'])
        def geneNames = getGeneNames(session['solrSearchFilter'])
        def transcriptGeneNames = getTranscriptGeneNames(session['solrSearchFilter'])
        def queryparameter=session['solrSearchFilter']

		def user=AuthUser.findByUsername(springSecurityService.getPrincipal().username)
		def secObjs=getExperimentSecureStudyList()
		
        if (isLink == "true") {
            def analysisId = params.analysisId
            analysisId = analysisId.toLong()
            response.setHeader('Content-disposition', 'attachment; filename=' + analysisId + "_ANALYSIS_DATA.txt")
            response.contentType = 'text/plain'
            PrintWriter dataWriter = new PrintWriter(response.writer);
            exportAnalysisData(analysisId,dataWriter,cutoff,regions,geneNames,transcriptGeneNames,0)

        } else if (isLink == "false") {
            def analysisIds = params?.analysisIds.split(",")
            def mailId = params.toMailId
            def link = new StringBuilder()
            if(queryparameter){link.append("Query Criteria at time of export: "+queryparameter+"\n")}
            link.append(createLink(controller: 'gwasSearch', action: 'exportAnalysis', absolute: true))
            def links = ""
            if (analysisIds.size() > 0) {

                for (analysisId in analysisIds) {
                    analysisId = analysisId.toLong()
					def analysis = BioAssayAnalysis.findById(analysisId, [max: 1])
					def access=gwasWebService.getGWASAccess(analysis.etlId, user)
					if(!secObjs.containsKey(analysis.etlId) || (!access.equals("Locked")  && !access.equals("VIEW"))){
						links += link+"?analysisId=" + analysisId + "&regions="+regions.toString().replace(" ","")+"&cutoff="+cutoff+"&geneNames="+geneNames.toString().replace(" ","")+"&isLink=true\n"
					}
					else{
						links += "Analysis " + analysis.name + " is a restricted study, you do not have permission to export.\n"
					}
                }
            }
            def messageSubject = "Export Analysis Results"  //(links,messageSubject,mailId)

            sendMail {
                to mailId
                subject messageSubject
                text links
            }

        } else {
            def analysisIds = params?.analysisIds.split(",")
            def mailId = params.toMailId
			def restrictedMsg=""
            def timestamp = new Date().format("yyyyMMddhhmmss")
            def rootFolder = "Export_" + timestamp
            String location = grailsApplication.config.grails.mail.attachments.dir
			String lineSeparator = System.getProperty('line.separator')
            String rootDir = location + File.separator+rootFolder 
			log.debug(rootDir +" is root directory");
			def analysisAIds=[]
			for(analysisId in analysisIds){
				analysisId = analysisId.toLong()
				def analysis = BioAssayAnalysis.findById(analysisId, [max: 1])
				def access=gwasWebService.getGWASAccess(analysis.etlId, user)
				if(!secObjs.containsKey(analysis.etlId) || (!access.equals("Locked")  && !access.equals("VIEW"))){
					analysisAIds.add(analysisId)
				}
				else{
					restrictedMsg += "Analysis " + analysis.name + "is a restricted study, you do not have permission to export.\n"				}
			}
            if (analysisIds.size() > 0) {

                for (analysisId in analysisAIds) {
                    analysisId = analysisId.toLong()
                    def analysis = BioAssayAnalysis.findById(analysisId, [max: 1])
                    def accession = analysis.etlId
                    def analysisName= analysis.name
                    Pattern pt = Pattern.compile("[^a-zA-Z0-9 ]")
                    Matcher match= pt.matcher(analysisName)
                    while(match.find()){
                        String s= match.group();
                        analysisName=analysisName.replaceAll("\\"+s, "");
						log.debug("After: "+analysisName)
                    }

                    def dirStudy = rootDir +File.separator+ accession + File.separator
					log.debug("Study "+dirStudy)
                    def dirAnalysis = dirStudy +analysisName + File.separator
					log.debug("Analysis "+ dirAnalysis)
                    def dir = new File(dirAnalysis)
                    dir.mkdirs()

                    //Creating Analysis Data file
                    def fileName = dirAnalysis+ analysisId + "_ANALYSIS_DATA.txt"
                    File file = new File(fileName);
                    BufferedWriter dataWriter = new BufferedWriter(new FileWriter(file));
                    exportAnalysisData(analysisId,dataWriter,cutoff,regions,geneNames,transcriptGeneNames,200)
//File.separator
                    //This is to generate a file with Study Metadata
                    def FileStudyMeta = dirStudy + accession + "_STUDY_METADATA.txt"
                    File FileStudy = new File(FileStudyMeta)
                    BufferedWriter dataWriterStudy = new BufferedWriter(new FileWriter(FileStudy))

                    def exp = Experiment.findByAccession(accession, [max: 1])
                    //dataWriterStudy.write accession

                    def formLayout = FormLayout.createCriteria().list() {
                        eq('key', 'study')
                        order('sequence', 'asc')
                    }
                    formLayout.each {
                        def dispName = it.displayName
                        dataWriterStudy.write dispName + ":"
                        if (it.column == 'accession') {
                            def add_col = exp.accession

                            dataWriterStudy.write add_col + "\n"
                        }
                        if (it.column == 'title') {
                            def add_col = exp.title
                            if (exp.title) {
                                dataWriterStudy.write add_col + "\n"
                            } else {
                                dataWriterStudy.write "\n"
                            }
                        }
                        if (it.column == 'description') {
                            def add_col = exp.description
                            if (exp.description) {
                                dataWriterStudy.write add_col + "\n"
                            } else {
                                dataWriterStudy.write "\n"
                            }
                        }
                        if (it.column == 'institution') {
                            def add_col = exp.institution
                            if (exp.institution) {
                                dataWriterStudy.write add_col + "\n"
                            } else {
                                dataWriterStudy.write "\n"
                            }
                        }
                        if (it.column == 'primaryInvestigator') {
                            def add_col = exp.primaryInvestigator
                            if (exp.primaryInvestigator) {
                                dataWriterStudy.write add_col + "\n"
                            } else {
                                dataWriterStudy.write "\n"
                            }
                        }
                        if (it.column == 'adHocPropertyMap.Study Short Name') {

                            def add_col = exp.getAdHocPropertyMap().get('Study Short Name')
                            if (exp.getAdHocPropertyMap().get('Study Short Name')) {
                                dataWriterStudy.write add_col + "\n"
                            } else {
                                dataWriterStudy.write "\n"
                            }
                        }
                        if (it.column == 'adHocPropertyMap.Data Availability') {
                            def add_col = exp.getAdHocPropertyMap().get('Data Availability')
                            if (exp.getAdHocPropertyMap().get('Data Availability')) {
                                dataWriterStudy.write add_col + "\n"
                            } else {
                                dataWriterStudy.write "\n"
                            }
                        }
                    }
                    dataWriterStudy.close()

                    def fileNameMeta = dirAnalysis + analysisId + "_ANALYSIS_METADATA.txt"


                    File fileMeta = new File(fileNameMeta);
                    BufferedWriter dataWriterMeta = new BufferedWriter(new FileWriter(fileMeta));
                    def layout = FormLayout.createCriteria().list() {
                        eq('key', 'analysis')
                        order('sequence', 'asc')
                    }
                    layout.each {
                        def dispName = it.displayName
                        if (analysis.assayDataType == 'EQTL' && it.column == 'phenotypes') {
                            dataWriterMeta.write "\nDiseases:"
                        } else if ((analysis.assayDataType == "EQTL" || analysis.assayDataType == "GWAS" || analysis.assayDataType == "GWAS Fail" || analysis.assayDataType == "Metabolic GWAS") && (it.column == 'pValueCutoff' || it.column == 'foldChangeCutoff')) {
                            //do nothing
                        } else {
                            dataWriterMeta.write "\n" + dispName + ":"
                        }
                        if (it.column == 'study') {
                            def add_col = exp.title
                            if (exp.title) {
                                dataWriterMeta.write add_col + ""
                            } else {
                                dataWriterMeta.write ""
                            }
                        } else if (it.column == 'phenotypes') {
                            analysis.diseases.disease.each() {
                                def add_col = it
                                dataWriterMeta.write add_col + ";"
                            }

                            if (!(analysis.assayDataType == "EQTL")) {
                                analysis.observations.name.each() {

                                    def add_col = it
                                    dataWriterMeta.write add_col + ";"
                                }
                            }
                        } else if (it.column == 'platforms') {
                            analysis.platforms.each() {
                                def add_col = it.vendor + ":" + it.name
                                dataWriterMeta.write add_col + ";"
                            }
                        } else if (it.column == 'name') {

                            def add_col = analysis.name
                            if (analysis.name) {
                                dataWriterMeta.write add_col + ""
                            } else {
                                dataWriterMeta.write ""
                            }
                        } else if (it.column == 'assayDataType') {

                            def add_col = analysis.assayDataType
                            if (analysis.assayDataType) {
                                dataWriterMeta.write add_col + ""
                            } else {
                                dataWriterMeta.write ""
                            }
                        } else if (it.column == 'shortDescription') {

                            def add_col = analysis.shortDescription
                            if (analysis.shortDescription) {
                                dataWriterMeta.write add_col + ""
                            } else {
                                dataWriterMeta.write ""
                            }
                        } else if (it.column == 'longDescription') {
                            def add_col = analysis.longDescription
                            if (analysis.longDescription) {
                                dataWriterMeta.write add_col + ""
                            } else {
                                dataWriterMeta.write ""
                            }
                        } else if (it.column == 'pValueCutoff') {

                            def add_col = analysis.pValueCutoff
                            if (analysis.pValueCutoff) {
                                dataWriterMeta.write add_col + ""
                            } else {
                                dataWriterMeta.write ""
                            }
                        } else if (it.column == 'foldChangeCutoff') {

                            def add_col = analysis.foldChangeCutoff
                            if (analysis.foldChangeCutoff) {
                                dataWriterMeta.write add_col + ""
                            } else {
                                dataWriterMeta.write ""
                            }
                        } else if (it.column == 'qaCriteria') {

                            def add_col = analysis.qaCriteria
                            if (analysis.qaCriteria) {
                                dataWriterMeta.write add_col + ""
                            } else {
                                dataWriterMeta.write ""
                            }
                        } else if (it.column == 'analysisMethodCode') {

                            def add_col = analysis.analysisMethodCode
                            if (analysis.analysisMethodCode) {
                                dataWriterMeta.write add_col + ""
                            } else {
                                dataWriterMeta.write ""
                            }
                        } else if (analysis.ext != null) {
                            if (it.column == 'ext.population') {
                                def add_col = analysis.ext.population
                                if (analysis.ext.population) {
                                    dataWriterMeta.write add_col + ""
                                } else {
                                    dataWriterMeta.write ""
                                }
                            } else if (it.column == 'ext.sampleSize') {

                                def add_col = analysis.ext.sampleSize
                                if (analysis.ext.sampleSize) {
                                    dataWriterMeta.write add_col + ""
                                } else {
                                    dataWriterMeta.write ""
                                }
                            } else if (it.column == 'ext.tissue') {

                                def add_col = analysis.ext.tissue
                                if (analysis.ext.tissue) {
                                    dataWriterMeta.write add_col + ""
                                } else {
                                    dataWriterMeta.write ""
                                }
                            } else if (it.column == 'ext.cellType') {

                                def add_col = analysis.ext.cellType
                                if (analysis.ext.cellType) {
                                    dataWriterMeta.write add_col + ""
                                } else {
                                    dataWriterMeta.write ""
                                }
                            } else if (it.column == 'ext.genomeVersion') {

                                def add_col = analysis.ext.genomeVersion
                                if (analysis.ext.genomeVersion) {
                                    dataWriterMeta.write add_col + ""
                                } else {
                                    dataWriterMeta.write ""
                                }
                            } else if (it.column == 'ext.researchUnit') {

                                def add_col = analysis.ext.researchUnit
                                if (analysis.ext.researchUnit) {
                                    dataWriterMeta.write add_col + ""
                                } else {
                                    dataWriterMeta.write ""
                                }
                            } else if (it.column == 'ext.modelName') {

                                def add_col = analysis.ext.modelName
                                if (analysis.ext.modelName) {
                                    dataWriterMeta.write add_col + ""
                                } else {
                                    dataWriterMeta.write ""
                                }
                            } else if (it.column == 'ext.modelDescription') {

                                def add_col = analysis.ext.modelDescription
                                if (analysis.ext.modelDescription) {
                                    dataWriterMeta.write add_col + ""
                                } else {
                                    dataWriterMeta.write ""
                                }
                            }
                        }
                    }
                    dataWriterMeta.close();

                }
            }



            File topDir = new File(rootDir)

            def zipFile = location +File.separator+ rootFolder + ".zip"
			
            ZipOutputStream zipOutput = new ZipOutputStream(new FileOutputStream(zipFile));

            int topDirLength = topDir.absolutePath.length()
			
            topDir.eachFileRecurse { file ->
                def relative = file.absolutePath.substring(topDirLength).replace('\\', '/')
                if (file.isDirectory() && !relative.endsWith('/')) {
                    relative += "/"
                }

                ZipEntry entry = new ZipEntry(relative)
                entry.time = file.lastModified()
                zipOutput.putNextEntry(entry)
                if (file.isFile()) {
                    zipOutput << new FileInputStream(file)
                }
            }

            zipOutput.close()

            //the path of the file e.g. : "c:/Users/nikos7/Desktop/myFile.txt"
            String messageBody = "Attached is the list of Analyses\n"+restrictedMsg
            String file = zipFile
            String messageSubject = "Export of Analysis as attachment"
            if (queryparameter) {messageBody+="Query Criteria at time of export: "+queryparameter+"\n"}
            file.substring(file.lastIndexOf('/')+1)
            sendMail {
                multipart true
                to mailId
                subject messageSubject
                text messageBody
                attach file.substring(file.lastIndexOf('/')+1), "application/zip", new File(file)
            }
        }

        def myString=new JSONObject().put("status","success").toString();
        render myString
    }
}
