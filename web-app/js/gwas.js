var REGION_DELIMITER = "^";

//This function will kick off the webservice that generates the QQ plot.
function loadQQPlot(analysisID)
{
    jQuery('#qqplot_results_' +analysisID).empty().addClass('ajaxloading');
    jQuery.ajax( {
        "url": getQQPlotURL,
        bDestroy: true,
        bServerSide: true,
        data: {analysisId: analysisID, pvalueCutoff: jQuery('#analysis_results_table_' + analysisID + '_cutoff').val(), search: jQuery('#analysis_results_table_' + analysisID + '_search').val()},
        "success": function ( json ) {
            jQuery('#analysis_holder_' +analysisID).unmask();
            jQuery('#qqplot_results_' + analysisID).prepend("<img src='" + json.imageURL + "' />").removeClass('ajaxloading');
            jQuery('#qqplot_export_' + analysisID).attr('href', json.imageURL);
        },
        "error": function ( xhr ) {
            jQuery('#qqplot_results_' + analysisID).append(xhr.responseText).removeClass('ajaxloading');
            jQuery('#analysis_holder_' +analysisID).unmask();
        },
        "dataType": "json"
    } );
}

// This function will load the analysis data into a GRAILS template.
function loadAnalysisResultsGrid(analysisID, paramMap)
{
    paramMap.analysisId = analysisID
    jQuery('#analysis_results_table_' + analysisID + '_wrapper').empty().addClass('ajaxloading');
    jQuery.ajax( {
        "url": getAnalysisDataURL,
        bDestroy: true,
        bServerSide: true,
        data: paramMap,
        "success": function (jqXHR) {
            jQuery('#analysis_holder_' +analysisID).unmask();
            jQuery('#analysis_results_table_' + analysisID + '_wrapper').html(jqXHR).removeClass('ajaxloading');
        },
        "error": function (jqXHR, error, e) {
            jQuery('#analysis_results_table_' + analysisID + '_wrapper').html(jqXHR).removeClass('ajaxloading');
            jQuery('#analysis_holder_' +analysisID).unmask();
        },
        "dataType": "html"
    } );
}

//This function will load all filtered analysis data into a GRAILS template.
function loadTableResultsGrid(paramMap)
{
    jQuery('#table-results-div').empty().addClass('ajaxloading');
    jQuery.ajax( {
        "url": getTableDataURL,
        bDestroy: true,
        bServerSide: true,
        data: paramMap,
        "success": function (jqXHR) {
            jQuery('#table-results-div').html(jqXHR).removeClass('ajaxloading');
        },
        "dataType": "html"
    } );
}

function startPlotter() {
    var selectedboxes = jQuery(".analysischeckbox:checked");
    if (selectedboxes.length == 0) {
        alert("No analyses are selected! Please select analyses to plot.");
    }
    else {
        var analysisIds = "";
        analysisIds += jQuery(selectedboxes[0]).attr('name');
        for (var i = 1; i < selectedboxes.length; i++) {
            analysisIds += "," + jQuery(selectedboxes[i]).attr('name');
        }

        var snpSource = jQuery('#plotSnpSource').val();
        var geneSource = jQuery('#plotGeneSource').val();
        var pvalueCutoff = jQuery('#plotPvalueCutoff').val();

        window.location = webStartURL + "?analysisIds=" + analysisIds + "&snpSource=" + snpSource + "&geneSource=GRCh37&pvalueCutoff=" + pvalueCutoff;
        jQuery('#divPlotOptions').dialog("destroy");
    }
}

function openPlotOptions() {
    var selectedboxes = jQuery(".analysischeckbox:checked");
    if (selectedboxes.length == 0) {
        alert("No analyses are selected! Please select analyses to plot.");
    }
    else {
        jQuery('#divPlotOptions').dialog("destroy");
        jQuery('#divPlotOptions').dialog(
            {
                modal: false,
                height: 250,
                width: 400,
                title: "Manhattan Plot Options",
                show: 'fade',
                hide: 'fade',
                resizable: false,
                buttons: {"Plot" : startPlotter}
            });
    }
}

function applyPopupFiltersRegions()
{
    //Pick out the useful fields and generate search terms

    var range = null;
    var basePairs = null;
    var version = null;
    var searchString = "";
    var text = "";
    var pValue= jQuery('#pValue').val();
    if (jQuery('[name=\'regionFilter\'][value=\'gene\']:checked').size() > 0) {
        jQuery("#filterGeneId :selected").each(function(i,selected){
            var geneId= selected.value

            var geneName = selected.text;
            range = jQuery('#filterGeneRange').val();
            basePairs = jQuery('#filterGeneBasePairs').val();
            basePairs=basePairs.replace(",","");

		if (basePairs == null || basePairs == "") {
                basePairs = 0;
            }

            use = jQuery('#filterGeneUse').val();
            searchString = "GENE" + REGION_DELIMITER + geneId

            text = "HG" + use + " " + geneName + " " + getRangeSymbol(range) + " " + basePairs;

            searchString += REGION_DELIMITER + range + REGION_DELIMITER + basePairs + REGION_DELIMITER + use;

            var searchParam={id:searchString,
                display:'Region',
                keyword:searchString,
                category:'REGION',
                text:text};

            addSearchTerm(searchParam, true);
        });
        //This destroys our popup window.
        updateSearch();
        jQuery(this).dialog("destroy");
    }
    else if (jQuery('[name=\'regionFilter\'][value=\'chromosome\']:checked').size() > 0) {
        range = jQuery('#filterChromosomeRange').val();
        basePairs = jQuery('#filterChromosomeBasePairs').val();
        if (basePairs == null || basePairs == "") {
            basePairs = 0;
        }
	basePairs=basePairs.replace(",","");
	        var whitespace =" ";
                if ((basePairs.match(/^[0-9]+\.[0-9]+$/) ||basePairs.match(/^[0-9]$/) || basePairs.match(/^\.[0-9]+$/)) && (basePairs.indexOf(whitespace) < 0)) {

        use = jQuery('#filterChromosomeUse').val();
        var chromNum = jQuery('#filterChromosomeNumber').val();
        var pos = jQuery('#filterChromosomePosition').val();
        if (pos == null || pos == "") {
            pos = 0;
        }

        searchString += "CHROMOSOME" + REGION_DELIMITER + chromNum + REGION_DELIMITER + use + REGION_DELIMITER + pos;

        if (pos != 0 && range != 0) {
            text = "HG" + use + " chromosome " + chromNum + " position " + pos + " " + getRangeSymbol(range) + " " + basePairs;
        }
        else {
            text = "HG" + use + " chromosome " + chromNum;
        }

        searchString += REGION_DELIMITER + range + REGION_DELIMITER + basePairs + REGION_DELIMITER + use;

        var searchParam={id:searchString,
            display:'Region',
            keyword:searchString,
            category:'REGION',
            text:text};

        addSearchTerm(searchParam);

        //This destroys our popup window.
        jQuery(this).dialog("destroy");
    }}
else
{
alert("Please enter numeric basepair value");
}
	var whitespace =" "
	if (pValue!="") {
		if ((pValue.match(/^[0-9]+\.[0-9]+$/) ||pValue.match(/^[0-9]$/) || pValue.match(/^\.[0-9]+$/)) && (pValue.indexOf(whitespace) < 0)) {
			var searchParam={id:'PVALUE'+REGION_DELIMITER+pValue,
            display:'PVALUE',
            keyword:'PVALUE'+REGION_DELIMITER+pValue,
            category:'PVALUE',
            text:pValue};
			addSearchTerm(searchParam);
			jQuery(this).dialog("destroy");
		}
		//This destroys our popup window.
		else { alert("Please enter numeric P-value") };
    }
    
}

function applyPopupFiltersEqtlTranscriptGene()
{
    jQuery("#filterEqtlTranscriptGeneId :selected").each(function(i,selected){
        var geneId= selected.value
        var geneName = selected.text;

        var searchParam={id:geneName,
            display:'Transcript Gene',
            keyword:geneName,
            category:'TRANSCRIPTGENE',
            text:geneName};

        addSearchTerm(searchParam, true);

    });
    var searchParamEQTL={id:'EQTL',
        display:'Data Types',
        keyword:'eQTL',
        category:'DATA_TYPE'};

    addSearchTerm(searchParamEQTL, true);

    jQuery(this).dialog("destroy");
    updateSearch();
}

function getRangeSymbol(string) {

    if (string == 'both') {
        return "+/-";
    }
    else if (string == 'plus') {
        return "+";
    }
    else if (string == 'minus') {
        return "-";
    }
}

jQuery(document).ready(function() {
    popupWindowPropertiesMap['Region of Interest'] = {'URLToUse': regionBrowseWindow, 'filteringFunction': applyPopupFiltersRegions, 'dialogHeight': 450, 'dialogWidth': 900}
    popupWindowPropertiesMap['eQTL Transcript Gene'] = {'URLToUse': eqtlTranscriptGeneWindow, 'filteringFunction': applyPopupFiltersEqtlTranscriptGene}
});
