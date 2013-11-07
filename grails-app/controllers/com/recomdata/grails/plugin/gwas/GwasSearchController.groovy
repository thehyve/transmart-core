package com.recomdata.grails.plugin.gwas

import bio.BioAssayAnalysis
import grails.converters.JSON
import search.GeneSignature
import search.GeneSignatureItem
import search.SearchKeyword

import java.lang.reflect.UndeclaredThrowableException

import static java.util.UUID.randomUUID

class GwasSearchController {

    def regionSearchService
    def RModulesFileWritingService
    def RModulesJobProcessingService
    def RModulesOutputRenderService

    /**
     * Renders a UI for selecting regions by gene/RSID or chromosome.
     */
    def getRegionFilter = {
        render(template:'/gwas/regionFilter', model: [ranges:['both':'+/-','plus':'+','minus':'-']], plugin: "transmartGwas")
    }

    def webStartPlotter = {

        def codebase = grailsApplication.config.com.recomdata.rwg.webstart.codebase
        def href = grailsApplication.config.com.recomdata.rwg.webstart.href
        def jar = grailsApplication.config.com.recomdata.rwg.webstart.jar
        def mainClass = grailsApplication.config.com.recomdata.rwg.webstart.mainClass
        def gInstance = "-services="+grailsApplication.config.com.recomdata.rwg.webstart.gwavaInstance
        def analysisIds = params.analysisIds
        def geneSource = params.geneSource
        def snpSource = params.snpSource
        def pvalueCutoff = params.pvalueCutoff
        def searchRegions = getWebserviceCriteria(session['solrSearchFilter'])
        def regionStrings = []
        for (region in searchRegions) {
            regionStrings += region[0] + "," + region[1]
        }
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

							    <jar href="./lib/BioServicesClient.jar"/>
							    <jar href="./lib/BioServicesUtil.jar"/>
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
							    <jar href="./lib/TDBApi.jar"/>
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
                        def region = r.split(";")
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

    def getRegionSearchResults(Long max, Long offset, Double cutoff, String sortField, String order, String search, List analysisIds) throws Exception {

        //Get list of REGION restrictions from session and translate to regions
        def regions = getSearchRegions(session['solrSearchFilter'])
        def geneNames = getGeneNames(session['solrSearchFilter'])

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
            gwasResult = runRegionQuery(analysisIds, regions, max, offset, cutoff, sortField, order, search, "gwas", geneNames)
        }
        if (hasEqtl) {
            eqtlResult = runRegionQuery(analysisIds, regions, max, offset, cutoff, sortField, order, search, "eqtl", geneNames)
        }

        return [gwasResults: gwasResult, eqtlResults: eqtlResult]
    }


    def runRegionQuery(analysisIds, regions, max, offset, cutoff, sortField, order, search, type, geneNames) throws Exception {

        //This will hold the index lookups for deciphering the large text meta-data field.
        def indexMap = [:]

        //Set a flag to record that the list was filtered by region
        def wasRegionFiltered = regions ? true : false

        def queryResult
        def analysisData = []
        def totalCount

        def columnNames = []
        def searchDAO = new GwasSearchDAO()

//		if (max > 0 && true) {
//			//If everything is the same as last time except the limits, return those rows out of the cache in the session
//			def cachedAnalysisData = session['cachedAnalysisData']
//			def cachedCount = session['cachedCount']
//			for (int i = offset+1; i < max+offset; i++) {
//				analysisData.push(cachedAnalysisData[i]);
//			}
//			totalCount = cachedCount
//		}
//		else if (true) {
//			//If the order is different, rerun the query but still use the cached count
//			queryResult = regionSearchService.getAnalysisData(analysisIds, regions, max, offset, cutoff, sortField, order, search, type, geneNames, false)
//			analysisData = queryResult.results
//			totalCount = session['cachedCount']
//			session['cachedAnalysisData'] = analysisData
//		}
        def wasShortcut = false
        if (!regions && !geneNames && analysisIds.size() == 1 && sortField.equals('null') && !cutoff && !search && max > 0) {
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
            queryResult = regionSearchService.getAnalysisData(analysisIds, regions, max, offset, cutoff, sortField, order, search, type, geneNames, true)
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
            analysisIndexData = searchDAO.getEqtlIndexData()
        }
        else {
            analysisIndexData = searchDAO.getGwasIndexData()
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
            columnNames.add(["sTitle":"Gene", "sortField":"data.gene"])
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
                        def largeTextField = it[3].split(";", -1)

                        //This will be the array that is reordered according to the meta-data index table.
                        //String[] newLargeTextField = new String[largeTextField.size()]
                        String[] newLargeTextField = new String[indexMap.size()]
                        def counter=0;
                        //Loop over the elements in the index map.
                        indexMap.each()
                                {
                                    //Reorder the array based on the index table.
                                    //if (it.key-1<newLargeTextField.size())
                                    if (it.key-1<largeTextField.size())
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


    def getQQPlotImage = {

        def returnJSON = [:]

        try {
            //We need to determine the data type of this analysis so we know where to pull the data from.
            def currentAnalysis = bio.BioAssayAnalysis.get(params.analysisId)

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

            //Initiate Data Access object to get to search data.
            def searchDAO = new GwasSearchDAO()

            //Get the GWAS Data. Call a different class based on the data type.
            def analysisData

            //Get the data from the index table for GWAS.
            def analysisIndexData

            def returnedAnalysisData = []

            //Get list of REGION restrictions from session and translate to regions
            def regions = getSearchRegions(session['solrSearchFilter'])
            def geneNames = getGeneNames(session['solrSearchFilter'])
            def analysisIds = [currentAnalysis.id]

            switch(currentAnalysis.assayDataType)
            {
                case "GWAS" :
                case "Metabolic GWAS" :
                    analysisData = regionSearchService.getAnalysisData(analysisIds, regions, 0, 0, pvalueCutoff, "null", "asc", search, "gwas", geneNames,false).results
                    analysisIndexData = searchDAO.getGwasIndexData()
                    break;
                case "EQTL" :
                    analysisData = regionSearchService.getAnalysisData(analysisIds, regions, 0, 0, pvalueCutoff, "null", "asc", search, "eqtl", geneNames,false).results
                    analysisIndexData = searchDAO.getEqtlIndexData()
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
                        def largeTextField = it[3].split(";", -1)

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
            def currentTempDirectory = RModulesFileWritingService.createTemporaryDirectory(uniqueName)

            def currentWorkingDirectory =  currentTempDirectory + File.separator + "workingDirectory" + File.separator

            //Write the data file for generating the image.
            def currentDataFile = RModulesFileWritingService.writeDataFile(currentWorkingDirectory, returnedAnalysisData,"QQPlot.txt")

            //Run the R script to generate the image file.
            RModulesJobProcessingService.runRScript(currentWorkingDirectory,"/QQ/QQPlot.R","create.qq.plot('QQPlot.txt')")

            //Verify the image file exists.
            def imagePath = currentWorkingDirectory + File.separator + "QQPlot.png"

            if(!new File(imagePath))
            {
                throw new Exception("Image file creation failed!")
            }
            else
            {
                //Move the image to the web directory so we can render it.
                def imageURL = RModulesOutputRenderService.moveImageFile(imagePath,uniqueName + ".png","QQPlots")

                returnJSON['imageURL'] = imageURL

                //Delete the working directory.
                def directoryToDelete = new File(currentTempDirectory)

                //This isn't working. I think something is holding the directory open? We need a way to clear out the temp files.
                directoryToDelete.deleteDir()

                //Render the image URL in a JSON object so we can reference it later.
                render returnJSON as JSON
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
                render(plugin: "transmartGwas", template: "/gwas/analysisResults", model: [analysisData: regionSearchResults.analysisData, columnNames: regionSearchResults.columnNames, max: regionSearchResults.max, offset: regionSearchResults.offset, cutoff: filter.cutoff, sortField: filter.sortField, order: filter.order, search: filter.search, totalCount: regionSearchResults.totalCount, wasRegionFiltered: regionSearchResults.wasRegionFiltered, wasShortcut: regionSearchResults.wasShortcut, analysisId: analysisId])
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

        session['filterTableView'] = filter

/*		if (analysisIds.size() >= 100) {
			render(text: "<p>The table view cannot be used with more than 100 analyses (${analysisIds.size()} analyses in current search results). Narrow down your results by adding filters.</p>")
			return
		}
		else*/ if (analysisIds.size() == 0) {
            render(text: "<p>No analyses were found for the current filter!</p>")
            return
        }
        else if (analysisIds[0] == -1) {
            render(text: "<p>To use the table view, please select a study or set of analyses from the filter browser in the left pane.</p>")
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
            render(plugin: "transmartGwas", template: "/gwas/gwasAndEqtlResults", model: [results: regionSearchResults, cutoff: filter.cutoff, sortField: filter.sortField, order: filter.order, search: filter.search])
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
                            limits = regionSearchService.getGeneLimits(geneId, ver)
                        }
                        else if (searchKeyword.dataCategory.equals("SNP")) {
                            limits = regionSearchService.getSnpLimits(geneId, ver)
                        }
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
                        def limits = regionSearchService.getGeneLimits(geneId, '19')
                        regions.push([gene: geneId, chromosome: limits.get('chrom'), low: limits.get('low'), high: limits.get('high'), ver: "19"])
                    }
                }
            }
            else if (s.startsWith("GENE")) {
                //If just plain genes, get the limits and default to HG19 as the version
                s = s.substring(5)
                def geneIds = s.split("\\|")
                for (geneString in geneIds) {
                    def geneId = geneString as long
                    def limits = regionSearchService.getGeneLimits(geneId, '19')
                    regions.push([gene: geneId, chromosome: limits.get('chrom'), low: limits.get('low'), high: limits.get('high'), ver: "19"])
                }
            }
            else if (s.startsWith("SNP")) {
                //If plain SNPs, as above (default to HG19)
                s = s.substring(4)
                def rsIds = s.split("\\|")
                for (rsId in rsIds) {
                    def limits = regionSearchService.getSnpLimits(rsId as long, '19')
                    regions.push([gene: rsId, chromosome: limits.get('chrom'), low: limits.get('low'), high: limits.get('high'), ver: "19"])
                }
            }
        }

        return regions
    }

    def getGeneNames(solrSearch) {
        def genes = []

        for (s in solrSearch) {
            if (s.startsWith("GENESIG")) {
                //Expand regions to genes and get their names
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
                    def geneId = geneString as long
                    def searchKeyword = SearchKeyword.get(geneId)
                    genes.push(searchKeyword.keyword)
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
}
