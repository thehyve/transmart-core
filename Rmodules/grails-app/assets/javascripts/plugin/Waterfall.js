/**
 * Register drag and drop.
 * Clear out all gobal variables and reset them to blank.
 */
function loadWaterfallView() {
    waterfallView.clear_high_dimensional_input('divDataNode');
    waterfallView.register_drag_drop();
}

// constructor
var WaterfallView = function () {
    RmodulesView.call(this);
};

// inherit RmodulesView
WaterfallView.prototype = new RmodulesView();

// correct the pointer
WaterfallView.prototype.constructor = WaterfallView;

// submit analysis job
WaterfallView.prototype.submit_job = function (form) {

    var dataNodeConceptCode = "";

    /**
     * Check if number is float
     * @param n
     * @returns {boolean}
     * @private
     */
    var _isNumberFloat = function (n) {
        return !isNaN(parseFloat(n)) && isFinite(n);
    };

    dataNodeConceptCode = readConceptVariables("divDataNode");

    var variablesConceptCode = dataNodeConceptCode;

    var mrnaData = false;
    var snpData = false;

    var fullGEXGeneList = "";
    var fullSNPGeneList = "";

    var dataNodeGeneList = window['divDataNodepathway'];
    var dataNodePlatform = window['divDataNodeplatforms'];
    var dataNodeType = window['divDataNodemarkerType'];

    var lowRangeOperator = document.getElementById('selLowRange').value;
    var highRangeOperator = document.getElementById('selHighRange').value;

    var lowRangeValue = document.getElementById('txtLowRange').value;
    var highRangeValue = document.getElementById('txtHighRange').value;

    //If we are using High Dimensional data we need to create variables that represent genes.
    if (dataNodeType == "Gene Expression") {
        //If the gene list already has items, add a comma.
        if (fullGEXGeneList != "") fullGEXGeneList += ",";

        //Add the genes in the list to the full list of GEX genes.
        fullGEXGeneList += dataNodeGeneList;

        //This flag will tell us to write the GEX text file.
        mrnaData = true;

        //Fix the platform to be something the R script expects.
        dataNodeType = "MRNA";
    }

    if (dataNodeType == "SNP") {
        //If the gene list already has items, add a comma.
        if (fullSNPGeneList != "") fullGEXGeneList += ",";

        //Add the genes in the list to the full list of SNP genes.
        fullSNPGeneList += dataNodeGeneList;

        //This flag will tell us to write the SNP text file.
        snpData = true;
    }

    //----------------------------------
    //Validation
    //----------------------------------
    //This is the independent variable.
    var dataNodeVariableEle = Ext.get("divDataNode");

    //Get the types of nodes from the input box.
    var dataNodeList = createNodeTypeArrayFromDiv(dataNodeVariableEle, "setnodetype");

    //Validate to make sure a concept was dragged in.
    if (dataNodeConceptCode == '') {
        Ext.Msg.alert('Missing input', 'Please drag at least one concept into the "Data Node" box.');
        return;
    }

    if ((this.isNumerical(dataNodeList) || this.isHd(dataNodeList)) && (dataNodeConceptCode.indexOf("|") != -1))
    {
        Ext.Msg.alert('Wrong input', 'For continuous data, you may only drag one node into the input boxes. ' +
            'The "Data Node" input box has multiple nodes.');
        return;
    }

    //If there is a value in the low bin field, make sure it is parsable as a number.
    if (lowRangeValue != '' && !_isNumberFloat(lowRangeValue)) {
        Ext.Msg.alert('Wrong input', 'The low range box needs to be left left blank or contain a valid number.');
        return;
    }

    //If there is a value in the low bin field, make sure it is parsable as a number.
    if (highRangeValue != '' && !_isNumberFloat(highRangeValue)) {
        Ext.Msg.alert('Wrong input', 'The high range box needs to be left left blank or contain a valid number.');
        return;
    }

    if (fullGEXGeneList == "" && dataNodeType == "MRNA") {
        Ext.Msg.alert("No Genes Selected!", "Please specify Genes in the Gene/Pathway Search box.")
        return false;
    }

    if (fullSNPGeneList == "" && dataNodeType == "SNP") {
        Ext.Msg.alert("No Genes Selected!", "Please specify Genes in the Gene/Pathway Search box.")
        return false;
    }
    //----------------------------------

    //If we don't have a platform, fill in Clinical.
    if (dataNodePlatform == null || dataNodePlatform == "") dataNodeType = "CLINICAL";

    var formParams = {
        dataNode: dataNodeConceptCode,
        divDataNodetimepoints: window['divDataNodetimepoints'],
        divDataNodesamples: window['divDataNodesamples'],
        divDataNoderbmPanels: window['divDataNoderbmPanels'],
        divDataNodeplatforms: dataNodePlatform,
        divDataNodegpls: window['divDataNodegplsValue'],
        divDataNodetissues: window['divDataNodetissues'],
        divDataNodeprobesAggregation: window['divDataNodeprobesAggregation'],
        divDataNodeSNPType: window['divDataNodeSNPType'],
        divDataNodeType: dataNodeType,
        divDataNodePathway: dataNodeGeneList,
        gexpathway: fullGEXGeneList,
        snppathway: fullSNPGeneList,
        divDataNodePathwayName: window['divDataNodepathwayName'],
        mrnaData: mrnaData,
        snpData: snpData,
        variablesConceptPaths: variablesConceptCode,
        lowRangeOperator: lowRangeOperator,
        highRangeOperator: highRangeOperator,
        lowRangeValue: lowRangeValue,
        highRangeValue: highRangeValue,
        jobType: 'Waterfall'
    };

    submitJob(formParams);
};

// init view instance
var waterfallView = new WaterfallView();

WaterfallView.prototype.select_input_as_cohort = function (form) {

    /**
     * Helper to convert select range value to string
     * @param selRangeValue
     * @returns {*}
     * @private
     */
    var _get_sel_range_value_as_string = function (selRangeValue) {
        var selRangeStr = null;
        if (selRangeValue == '<') selRangeStr = 'LT';
        else if (selRangeValue == '<=') selRangeStr = 'LE';
        else if (selRangeValue == '=') selRangeStr = 'EQ';
        else if (selRangeValue == '>') selRangeStr = 'GT';
        else if (selRangeValue == '>=') selRangeStr = 'GE';

        return selRangeStr
    };

    for (var s = 1; s <= GLOBAL.NumOfSubsets; s++) {
        for (var d = 1; d <= GLOBAL.NumOfQueryCriteriaGroups; d++) {
            var qcd = Ext.get("queryCriteriaDiv" + s + '_' + d.toString());
            if (s == 1 && qcd.dom.childNodes.length == 0) {
                var lowRangeConcept = createPanelItemNew(qcd, analysisConcept.concept);
                setValue(lowRangeConcept, 'numeric', _get_sel_range_value_as_string(form.selLowRange.value), 'N', '',
                    form.txtLowRange.value, 'ratio');
                break;
            } else if (s == 2 && qcd.dom.childNodes.length == 0) {
                var highRangeConcept = createPanelItemNew(qcd, analysisConcept.concept);
                setValue(highRangeConcept, 'numeric', _get_sel_range_value_as_string(form.selHighRange.value), 'N', '',
                    form.txtHighRange.value, 'ratio');
                break;
            }
        }
    }

    Ext.getCmp('queryPanel').show();
};
