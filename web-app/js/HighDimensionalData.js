var HighDimensionalData = function () {

    /**
     * Get supported high dimensional data types
     * @returns {*}
     */
    var getSupportedTypes = function () {

        // check if tranSMART's global variable exists then use it
        if (typeof(HIGH_DIMENSIONAL_DATA) != "undefined") {
            return HIGH_DIMENSIONAL_DATA
        }

        return {
            "mrna"          : {"platform": "MRNA_AFFYMETRIX",   "type": "Gene Expression"},
            "mirna_qpcr"    : {"platform": "MIRNA_QPCR",        "type": "MIRNA_QPCR"},
            "mirna_seq"     : {"platform": "MIRNA_SEQ",         "type": "MIRNA_SEQ"},
            "rbm"           : {"platform": "RBM",               "type": "RBM"},
            "proteomics"    : {"platform": "PROTEIN",           "type": "PROTEOMICS"},
            "snp"           : {"platform": "SNP",               "type": "SNP"},
            "rnaseq"        : {"platform": "RNA_AFFYMETRIX",    "type": "RNASEQ"},
            "metabolite"    : {"platform": "METABOLOMICS",      "type": "METABOLOMICS"}
        };
    }

    // define supported types
    this.supportedTypes = getSupportedTypes();

    // high dimensional data view element
    this.view = null;

    // high dimensional data
    this.data = null;

    // div id
    this.divId = null;
    
    this.hideAggregration = null;
}

/**
 * Populate data to the popup window
 */
HighDimensionalData.prototype.populate_data = function () {
    for (var key in this.data) {
        if (this.data.hasOwnProperty(key)) {

            var _tmp_data = this.data[key];

            // set global marker type
            if (_tmp_data.platforms[0].markerType) {
                GLOBAL.HighDimDataType = _tmp_data.platforms[0].markerType;
            } else {
                GLOBAL.HighDimDataType = "";
            }

            if (document.getElementById("highDimContainer")) {

                document.getElementById("highDimensionType").value = key;
                document.getElementById("platforms1").value = GLOBAL.HighDimDataType;
                document.getElementById("gpl1").value = _tmp_data.platforms[0].id ? _tmp_data.platforms[0].id : "";
                document.getElementById("sample1").value = _tmp_data.sampleTypes[0].label ? _tmp_data.sampleTypes[0].label : "";

                var _strTissueTypes = "";

                if (_tmp_data.tissueTypes) {
                    for (var i = 0, max = _tmp_data.tissueTypes.length; i < max; i++) {
                        if (_tmp_data.tissueTypes[i].label) {
                            _strTissueTypes += _tmp_data.tissueTypes[i].label.concat((i < max - 1) ? ", " : "");
                        } else {
                            _strTissueTypes += "";
                        }
                    }
                    document.getElementById("tissue1").value = _strTissueTypes;
                }

                this.create_pathway_search_box('searchPathway', 'divpathway');
                
            	document.getElementById("probesAggregation").checked = false;
                if(this.hideAggregration){
                	document.getElementById("probesAggregationDiv").style.visibility = "hidden";
                }else{
                	document.getElementById("probesAggregationDiv").style.visibility = "visible";
                }
            }

        } else {
            Ext.Msg.alert("Error", "Returned object is unknown.");
        }
    }
}

HighDimensionalData.prototype.create_pathway_search_box = function (searchInputEltName, divName) {

    var ajaxurl, ds, resultTpl;

    // remove all elements
    var el = document.getElementById(searchInputEltName);
    if (el) {
        el.value = '';   // empty the search input value
        // then remove all child elements
        while (el.firstChild) {
            el.removeChild(el.firstChild);
        }
    }

    ajaxurl = pageInfo.basePath + '/search/loadSearchPathways';
    ds = new Ext.data.Store({
        proxy: new Ext.data.ScriptTagProxy({
            url: ajaxurl
        }),
        reader: new Ext.data.JsonReader(
            {root: "rows", id: "id"},
            [
                {name: "id"},
                {name: "source"},
                {name: "keyword"},
                {name: "synonyms"},
                {name: "category"},
                {name: "display"}
            ]
        )
    });

    // Custom rendering Template
    resultTpl = new Ext.XTemplate(
        '<tpl for=".">',
        '<div class="search-item">',
        '<p>',
        '<span class="category-{display:lowercase}">{display}&gt;{source}</span>&nbsp;',
        '<b>{keyword}</b>&nbsp; {synonyms}',
        '</p>',
        '</div>',
        '</tpl>'
    );

    var search = new Ext.form.ComboBox({
        store: ds,
        displayField: 'title',
        width: 455,
        typeAhead: false,
        loadingText: 'Searching...',
        listHeight: 500,
        valueField: 'naturalid',
        hideTrigger: true,
        allowBlank: false,
        name: 'searchText',
        mode: 'remote',
        tpl: resultTpl,
        minChars: 1,
        applyTo: searchInputEltName,
        itemSelector: 'div.search-item',

        listeners: {
            'beforequery': function (queryEvent) {
                // Use the last element in the query string as the query
                var keywords = queryEvent.query.split(/[,\s]\s*/);
                queryEvent.query = keywords[keywords.length - 1];
            }
        },

        onSelect: function (record) {
            // Check for duplicates
            var ids = GLOBAL.CurrentPathway.split(",");
            if (ids.indexOf(record.data.id.toString()) == -1) {

                // Append the selected keyword to the list
                if (GLOBAL.CurrentPathway) {
                    GLOBAL.CurrentPathway += ",";
                    GLOBAL.CurrentPathwayName += ", ";
                }
                GLOBAL.CurrentPathway += record.data.id.toString();
                GLOBAL.CurrentPathwayName += record.data.keyword;
            }

            // Set the value in the text field
            var sp = Ext.get(searchInputEltName);
            sp.dom.value = GLOBAL.CurrentPathwayName;

            search.collapse();
        }
    });

    if (GLOBAL.HeatmapType == 'Select' || GLOBAL.HeatmapType == 'PCA') {
        //Clear the pathway variable so we don't submit a value.
        GLOBAL.CurrentPathway = '';
        //Remove the pathway box.
        document.getElementById(divName).style.display = "none";
    }
}

HighDimensionalData.prototype.generate_view = function () {

    var _this = this;
    var _view = this.view;

    /**
     * to satisfy load high dim function
     * @private
     */
    var _store_high_dim_params_as_global = function () {

        window[_this.divId + 'pathway'] = GLOBAL.CurrentPathway;
        window[_this.divId + 'pathwayName'] = GLOBAL.CurrentPathwayName;
        window[_this.divId + 'markerType'] = GLOBAL.HighDimDataType;

        window[_this.divId + 'samples1'] = Ext.get('sample1').dom.value;
        window[_this.divId + 'platforms1'] = Ext.get('platforms1').dom.value;
        window[_this.divId + 'gpls1'] = Ext.get('gpl1').dom.value;
        window[_this.divId + 'tissues1'] = Ext.get('tissue1').dom.value;

        window[_this.divId + 'probesAggregation'] = Ext.get('probesAggregation').dom.checked;

    };

    /**
     *  Inner function to display node details summary
     * @private
     */
    var _display_high_dim_selection_summary = function () {

        // set high dimensional data type
        if (_this.divId == 'divIndependentVariable' && document.getElementById("independentVarDataType")) {
            document.getElementById("independentVarDataType").value = Ext.get('highDimensionType').dom.value;
            document.getElementById("independentPathway").value = GLOBAL.CurrentPathway;
        }
        if (_this.divId == 'divDependentVariable' && document.getElementById("dependentVarDataType")) {
            document.getElementById("dependentVarDataType").value = Ext.get('highDimensionType').dom.value;
            document.getElementById("dependentPathway").value = GLOBAL.CurrentPathway;
        }
        if (_this.divId == 'divCategoryVariable' && document.getElementById("dependentVarDataType")) {
            document.getElementById("dependentVarDataType").value = Ext.get('highDimensionType').dom.value;
            document.getElementById("dependentPathway").value = GLOBAL.CurrentPathway;
        }
        if (_this.divId == 'divGroupByVariable' && document.getElementById("groupByVarDataType")) {
            document.getElementById("groupByVarDataType").value = Ext.get('highDimensionType').dom.value;
            document.getElementById("groupByPathway").value = GLOBAL.CurrentPathway;
        }
        // init summary string
        var summaryString = '<br> <b>GPL Platform:</b> ' + Ext.get('gpl1').dom.value +
            '<br> <b>Sample:</b> ' + Ext.get('sample1').dom.value +
            '<br> <b>Tissue:</b> ' + Ext.get('tissue1').dom.value +
            '<br>';

        // get search gene/pathway
        var selectedSearchPathway = GLOBAL.CurrentPathwayName;

        // get flag for probe aggregation
        var probeAggregationFlag = Ext.get('probesAggregation').dom.checked;

        // create final string
        var innerHtml = summaryString +
            '<br> <b>Pathway:</b> ' + selectedSearchPathway +
            '<br> <b>Probe aggregation:</b> ' + probeAggregationFlag +
            '<br> <b>Marker Type:</b> ' + GLOBAL.HighDimDataType;

        // ** start stub **
        // TODO : to be removed when load high dim params is no longer used.
        _store_high_dim_params_as_global();
        // ** end stub **

        // display it
        var domObj = document.getElementById("display" + GLOBAL.CurrentAnalysisDivId);
        domObj.innerHTML = innerHtml;
    };

    /**
     * Inner function to create High Dimensional Popup element
     * @returns {Ext.Window}
     * @private
     */
    var _create_view = function () {
        return new Ext.Window({

            id: 'compareStepPathwaySelectionWindow',
            title: 'Compare Subsets-Pathway Selection',
            layout: 'fit',
            width: 475,
            autoHeight: true,
            closable: false,
            plain: true,
            modal: true,
            border: false,
            buttons: [
                {
                    id: 'dataAssociationApplyButton',
                    text: 'Apply Selections',
                    handler: function () {
                        _display_high_dim_selection_summary();
                        _view.hide();
                    }
                },
                {
                    text: 'Cancel',
                    handler: function () {
                        _view.hide();
                    }
                }
            ],
            resizable: false,
            autoLoad: {
                url: pageInfo.basePath + '/static/panels/highDimensionalWindow.html',
                scripts: true,
                nocache: true,
                discardUrl: true,
                method: 'GET'
            },
            tools: [
                {
                    id: 'help',
                    qtip: 'Click for context sensitive help',
                    handler: function (event, toolEl, panel) {
                        D2H_ShowHelp('1126', helpURL, "wndExternal", CTXT_DISPLAY_FULLHELP);
                    }
                }
            ]
        });
    }

    // ------------------------------------------- //
    // create view only when it's not created yet. //
    // ------------------------------------------- //

    if (!_view) {
        _view = _create_view();
    }

    return _view;
}

HighDimensionalData.prototype.get_inputs = function (divId) {
    return [
        {
            "label": "High Dimensional Data",
            "el": Ext.get(divId),
            "validations": [
                {type: "REQUIRED"},
                {type: "HIGH_DIMENSIONAL"}
            ]
        }
    ]
}

HighDimensionalData.prototype.gather_high_dimensional_data = function (divId, hideAggregration) {

    var _this = this;
    this.hideAggregration=hideAggregration;
    /**
     * Reset global variables
     * @private
     */
    var _reset_global_var = function () {

        // reset the pathway information.
        GLOBAL.CurrentPathway = '';
        GLOBAL.CurrentPathwayName = '';

        // set global div id
        GLOBAL.CurrentAnalysisDivId = divId;
        _this.divId = divId;

    }

    _reset_global_var();

    // check if global subset id is already defined or not
    // if not the re-run query
    if (!variableDivEmpty(divId)
        && ((GLOBAL.CurrentSubsetIDs[1] == null) || (multipleSubsets() && GLOBAL.CurrentSubsetIDs[2] == null))) {
        runAllQueriesForSubsetId(function () {
            _this.gather_high_dimensional_data(divId, hideAggregration);
        }, divId);
        return;
    }

    // reset data
    _this.data = null;

    // instantiate input elements object with their corresponding validations
    var inputArray = this.get_inputs(divId);
    // define the validator for this form
    var formValidator = new FormValidator(inputArray);

    if (formValidator.validateInputForm()) {
      this.fetchNodeDetails( divId, function( result ) {
        _this.data = JSON.parse(result.responseText);

        _this.display_high_dimensional_popup();
//        TODO: re-enable platform validation, except for geneprint:
//        platforms = _this.getPlatformValidator(_this.getPlatforms(_this.data));
//        var formValidator = new FormValidator(platforms);
//
//        if (formValidator.validateInputForm()) {
//          _this.display_high_dimensional_popup();
//        } else {
//          formValidator.display_errors();
//        }

      });
    } else { // something is not correct in the validation
        // display the error message
        formValidator.display_errors();
    }
}

HighDimensionalData.prototype.getPlatformValidator = function(platforms) {
    return [
        {
            "label": "Platforms",
            "el": platforms,
            "validations": [
                {type: "IDENTICAL_ITEMS"}
            ]
        }
    ]
}

HighDimensionalData.prototype.getPlatforms = function(data) {
    var keys = Object.keys(data);
    var platformTitles = [];

    for (var i = 0; i < keys.length; i++) {
        var dataTypeSpecificTitles = data[keys[i]].platforms.map( function(platform) {
            return platform.title;
        });
        platformTitles = platformTitles.concat(dataTypeSpecificTitles);
    }

    return platformTitles;
}

HighDimensionalData.prototype.fetchNodeDetails = function( divId, callback ) {
    // get nodes from the dropzone
    var _nodes = Ext.get(divId).dom.childNodes;

    var _conceptPaths = new Array();

    for (var i = 0; i < _nodes.length; i++) {
        var _str_key = _nodes[i].concept.key;
        _conceptPaths.push(_str_key);
    }

    // Retrieve node details
    Ext.Ajax.request({
        url: pageInfo.basePath + "/HighDimension/nodeDetails",
        method: 'POST',
        timeout: '10000',
        params: Ext.urlEncode({
            conceptKeys: _conceptPaths
        }),
        success: callback,
        failure: function () {
            Ext.Msg.alert("Error", "Cannot retrieve high dimensional node details");
        }
    }); 
}

HighDimensionalData.prototype.load_parameters = function (formParams) {
    //These will tell tranSMART what data types we need to retrieve.
    var mrnaData = false
    var snpData = false

    //Gene expression filters.
    var fullGEXSampleType = "";
    var fullGEXTissueType = "";
    var fullGEXTime = "";
    var fullGEXGeneList = "";
    var fullGEXGPL = "";

    //SNP Filters.
    var fullSNPSampleType = "";
    var fullSNPTissueType = "";
    var fullSNPTime = "";
    var fullSNPGeneList = "";
    var fullSNPGPL = "";

    //Pull the individual filters from the window object.
    var independentGeneList = document.getElementById('independentPathway').value
    var dependentGeneList = document.getElementById('dependentPathway').value

    var dependentPlatform = window['divDependentVariableplatforms1'];
    var independentPlatform = window['divIndependentVariableplatforms1'];

    var dependentType = window['divDependentVariablemarkerType'];
    var independentType = window['divIndependentVariablemarkerType'];

    var dependentTime = window['divDependentVariabletimepointsValues'];
    var independentTime = window['divIndependentVariabletimepointsValues'];

    var dependentSample = window['divDependentVariablesamplesValues'];
    var independentSample = window['divIndependentVariablesamplesValues'];

    var dependentTissue = window['divDependentVariabletissuesValues'];
    var independentTissue = window['divIndependentVariabletissuesValues'];

    var dependentGPL = window['divDependentVariablegplValues'];
    var independentGPL = window['divIndependentVariablegplValues'];

    if (dependentGPL) dependentGPL = dependentGPL[0];
    if (independentGPL) independentGPL = independentGPL[0];

    // If we are using High Dimensional data we need to create variables that represent genes from both independent and
    // dependent selections (In the event they are both of a single high dimensional type).
    // Check to see if the user selected GEX in the independent input.
    if (independentType == "Gene Expression") {
        //Put the independent filters in the GEX variables.
        fullGEXGeneList = String(independentGeneList);
        fullGEXSampleType = String(independentSample);
        fullGEXTissueType = String(independentTissue);
        fullGEXTime = String(independentTime);
        fullGEXGPL = String(independentGPL);

        //This flag will tell us to write the GEX text file.
        mrnaData = true;

        //Fix the platform to be something the R script expects.
        independentType = "MRNA";
    }

    if (dependentType == "Gene Expression") {
        //If the gene list already has items, add a comma.
        if (fullGEXGeneList != "") fullGEXGeneList += ","
        if (fullGEXSampleType != "") fullGEXSampleType += ","
        if (fullGEXTissueType != "") fullGEXTissueType += ","
        if (fullGEXTime != "") fullGEXTime += ","
        if (fullGEXGPL != "") fullGEXGPL += ","

        //Add the genes in the list to the full list of GEX genes.
        fullGEXGeneList += String(dependentGeneList);
        fullGEXSampleType += String(dependentSample);
        fullGEXTissueType += String(dependentTissue);
        fullGEXTime += String(dependentTime);
        fullGEXGPL += String(dependentGPL);

        //This flag will tell us to write the GEX text file.
        mrnaData = true;

        //Fix the platform to be something the R script expects.
        dependentType = "MRNA";
    }

    //Check to see if the user selected SNP in the independent input.
    if (independentType == "SNP") {
        //The genes entered into the search box were SNP genes.
        fullSNPGeneList = String(independentGeneList);
        fullSNPSampleType = String(independentSample);
        fullSNPTissueType = String(independentTissue);
        fullSNPTime = String(independentTime);
        fullSNPGPL = String(independentGPL);

        //This flag will tell us to write the SNP text file.
        snpData = true;
    }

    if (dependentType == "SNP") {
        //If the gene list already has items, add a comma.
        if (fullSNPGeneList != "") fullSNPGeneList += ","
        if (fullSNPSampleType != "") fullSNPSampleType += ","
        if (fullSNPTissueType != "") fullSNPTissueType += ","
        if (fullSNPTime != "") fullSNPTime += ","
        if (fullSNPGPL != "") fullSNPGPL += ","

        //Add the genes in the list to the full list of SNP genes.
        fullSNPGeneList += String(dependentGeneList)
        fullSNPSampleType += String(dependentSample);
        fullSNPTissueType += String(dependentTissue);
        fullSNPTime += String(dependentTime);
        fullSNPGPL += dependentGPL;

        //This flag will tell us to write the SNP text file.
        snpData = true;
    }

    if (!independentGeneList && independentType || !dependentGeneList && dependentType) {
        Ext.Msg.alert("No Filter Selected", "Please specify Gene/Pathway/mirID/UniProtID in High Dimensional Data pop-up.")
        return false;
    }

    var _dependentDataType = document.getElementById('dependentVarDataType').value ? document.getElementById('dependentVarDataType').value : 'CLINICAL';
    var _independentDataType = document.getElementById('independentVarDataType').value ? document.getElementById('independentVarDataType').value : 'CLINICAL';

    formParams["divDependentVariabletimepoints"] = window['divDependentVariabletimepoints1'];
    formParams["divDependentVariablesamples"] = window['divDependentVariablesamples1'];
    formParams["divDependentVariablerbmPanels"] = window['divDependentVariablerbmPanels1'];
    formParams["divDependentVariableplatforms"] = dependentPlatform
    formParams["divDependentVariablegpls"] = window['divDependentVariablegplsValue1'];
    formParams["divDependentVariabletissues"] = window['divDependentVariabletissues1'];
    formParams["divDependentVariableprobesAggregation"] = window['divDependentVariableprobesAggregation'];
    formParams["divDependentVariableSNPType"] = window['divDependentVariableSNPType'];
    formParams["divDependentVariableType"] = _dependentDataType;
    formParams["divDependentVariablePathway"] = dependentGeneList;
    formParams["divIndependentVariabletimepoints"] = window['divIndependentVariabletimepoints1'];
    formParams["divIndependentVariablesamples"] = window['divIndependentVariablesamples1'];
    formParams["divIndependentVariablerbmPanels"] = window['divIndependentVariablerbmPanels1'];
    formParams["divIndependentVariableplatforms"] = independentPlatform;
    formParams["divIndependentVariablegpls"] = window['divIndependentVariablegplsValue1'];
    formParams["divIndependentVariabletissues"] = window['divIndependentVariabletissues1'];
    formParams["divIndependentVariableprobesAggregation"] = window['divIndependentVariableprobesAggregation'];
    formParams["divIndependentVariableSNPType"] = window['divIndependentVariableSNPType'];
    formParams["divIndependentVariableType"] = _independentDataType;
    formParams["divIndependentVariablePathway"] = independentGeneList;
    formParams["gexpathway"] = fullGEXGeneList;
    formParams["snppathway"] = fullSNPGeneList;
    formParams["divIndependentPathwayName"] = window['divIndependentVariablepathwayName'];
    formParams["divDependentPathwayName"] = window['divDependentVariablepathwayName'];
    formParams["mrnaData"] = mrnaData;
    formParams["snpData"] = snpData;
    formParams["gexgpl"] = fullGEXGPL;
    formParams["snpgpl"] = fullSNPGPL;

    return true;
}

HighDimensionalData.prototype.display_high_dimensional_popup = function () {

    // generate view and populate it with the data
    this.view = this.generate_view();
    // then show it
    if (typeof viewport !== undefined) {
        this.view.show(viewport, this.populate_data());
    } else {
        console.error("No view port to display the window.");
    }

}

var highDimensionalData = new HighDimensionalData();
