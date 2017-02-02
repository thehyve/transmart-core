package annotation

import grails.converters.JSON
import org.transmart.biomart.BioAssayPlatform
import org.transmart.biomart.ConceptCode


class MetaDataController {

    def formLayoutService
    def amTagTemplateService
    def amTagItemService
    def fmFolderService
    def ontologyService
    def searchKeywordService
    def solrFacetService
    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index = {
        redirect(action: "list", params: params)
    }

    def list = {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)

    }

    def searchAction =
            {
                redirect(action: "list", params: params)
            }

    /**
     * Find the top 15 diseases with a case-insensitive LIKE
     */
    def extSearch = {
        log.info "EXT SEARCH called"
        def paramMap = params
        log.info params.toMapString()

        def value = params.term ? params.term.toUpperCase() : ''
        def codeTypeName = params.codeTypeName ? params.codeTypeName : '';

        def conceptCodes = ConceptCode.executeQuery("FROM ConceptCode cc WHERE cc.codeTypeName = :codeTypeName and  upper(cc.codeName) LIKE :codeName order by codeTypeName", [codeTypeName: codeTypeName, codeName: value + "%"], [max: 10]);

        log.info "There are " + conceptCodes.size() + " " + params.codeTypeName + " records found in ConceptCode"
        def itemlist = [];
        for (conceptCode in conceptCodes) {
            if (conceptCode.uniqueId != null && conceptCode.codeName != null) {
                itemlist.add([id: conceptCode.uniqueId, keyword: conceptCode.codeName, sourceAndCode: conceptCode.uniqueId, category: "", display: ""]);
            }
        }

        render itemlist as JSON;
    }

    /**
     * Find the top 15 compounds with a case-insensitive LIKE
     */
    def bioCompoundSearch = {
        log.info "EXT bioCompoundSearch called"
        def paramMap = params
        log.info params.toMapString()
        render searchKeywordService.findSearchKeywords("COMPOUND", params.term, 10) as JSON

    }
    /**
     * Find the top 15 diseases with a case-insensitive LIKE
     */
    def bioDiseaseSearch = {
        log.info "EXT bioDiseaseSearch called"
        def paramMap = params
        log.info params.toMapString()
        render searchKeywordService.findSearchKeywords("DISEASE", params.term, 10) as JSON

    }

    /**
     * Find the top 15 genes with a case-insensitive LIKE
     */
    def bioMarkerSearch = {
        log.info "EXT bioMarkerSearch called"
        def paramMap = params
        log.info params.toMapString()

        render searchKeywordService.findSearchKeywords("GENE", params.term, 10) as JSON
    }

    /**
     * Find the top 15 biosources with a case-insensitive LIKE
     */
    def biosourceSearch = {
        log.info "EXT biosourceSearch called"
        def paramMap = params
        log.info params.toMapString()
        render searchKeywordService.findSearchKeywords("BIOSOURCE", params.term, 10) as JSON

    }

    /**
     * Find the top 15 diseases, genes, pathways or observations with a case-insensitive LIKE
     */
    def programTargetSearch = {
        log.info "EXT programTargetSearch called"
        def paramMap = params
        log.info params.toMapString()
        def itemlist = [];

        def value = params.term ? params.term.toUpperCase() : ''

        def diseaseJSON = searchKeywordService.findSearchKeywords("DISEASE", params.term, 10) as JSON
        def list = JSON.parse(diseaseJSON.toString())
        list.each { itemlist.add(it) }
        def geneJSON = searchKeywordService.findSearchKeywords("GENE", params.term, 10) as JSON
        list = JSON.parse(geneJSON.toString())
        list.each { itemlist.add(it) }
        def pathwayJSON = searchKeywordService.findSearchKeywords("PATHWAY", params.term, 10) as JSON
        list = JSON.parse(pathwayJSON.toString())
        list.each { itemlist.add(it) }
        def observationJSON = searchKeywordService.findSearchKeywords("OBSERVATION", params.term, 10) as JSON
        list = JSON.parse(observationJSON.toString())
        list.each { itemlist.add(it) }
        def conceptCodes = ConceptCode.executeQuery("FROM ConceptCode cc WHERE cc.codeTypeName = :codeTypeName and  upper(cc.codeName) LIKE :codeName order by codeTypeName", [codeTypeName: 'PROGRAM_TARGET_PATHWAY_PHENOTYPE', codeName: value + "%"], [max: 10]);
        for (conceptCode in conceptCodes) {
            itemlist.add([id: conceptCode.uniqueId, label: conceptCode.codeName, sourceAndCode: conceptCode.uniqueId, categoryId: "PROGRAM_TARGET", category: "Program Target", display: ""]);
        }

        render itemlist as JSON;
    }

    def bioAssayPlatformSearch = {
        log.info "EXT platformSearch called"
        log.info params.toMapString()
        Map pagingMap = [max: 20];

        def paramMap = [:];
        def itemlist = [];

        def value = params.term.toUpperCase();
        StringBuffer sb = new StringBuffer();
        sb.append("from BioAssayPlatform p where 1=1 ");

        if (value != null && value != "null") {
            sb.append(" and upper(p.name) like :term ");
            paramMap.put("term", value + "%");
        }

        if (params.vendor != null && params.vendor != "null") {
            sb.append(" and p.vendor = :vendor ");
            paramMap.put("vendor", params.vendor);
        }

        if (params.measurement != null && params.measurement != "null") {
            sb.append(" and p.platformType = :measurement ");
            paramMap.put("measurement", params.measurement);
        }

        if (params.technology != null && params.technology != "null") {
            sb.append(" and p.platformTechnology = :technology ");
            paramMap.put("technology", params.technology);
        }

        // sort
        sb.append("order by platformType, vendor, platformTechnology, name");

        log.info "SB == " + sb.toString()
        log.info "paramMap = " + paramMap.toMapString()

        def platforms = BioAssayPlatform.executeQuery(sb.toString(), paramMap, pagingMap);
        // .executeQuery("from BioAssayPlatform p WHERE upper(p.name) LIKE :term  order by platformType, vendor, platformTechnology, name", [term: value+'%'], [max: 20]);
        log.info platforms
        for (platform in platforms) {
            String displayString = platform.name
            //+ " -- [MEASUREMENT::"+platform.platformType + " VENDOR::" + platform.vendor + " TECH::" + platform.platformTechnology + "]"
            String filterString = ""

            if (params.measurement == null || params.measurement == "null" || params.measurement == "") {
                filterString += " MEASUREMENT::" + platform.platformType;
            }

            if (params.technology == null || params.technology == "null" || params.technology == "") {
                filterString += " TECHNOLOGY::" + platform.platformTechnology;
            }

            if (params.vendor == null || params.vendor == "null" || params.vendor == "") {
                filterString += " VENDOR::" + platform.vendor;
            }

            if (filterString != "") {
                filterString = " -- [" + filterString + "]"
            }
            displayString = displayString + filterString
            itemlist.add([id: platform.uniqueId, label: displayString, category: "PLATFORM", display: "Platform"]);
        }


        render itemlist as JSON;
    }

}
