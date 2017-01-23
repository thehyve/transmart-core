/**
 * Where everything starts
 */
function loadLineGraphView() {
    lineGraphView.register_drag_drop();
    lineGraphView.clear_high_dimensional_input('divGroupByVariable');
    lineGraphView.clear_high_dimensional_input('divDependentVariable');
    lineGraphView.toggle_binning();
}

/**
 * Constructor
 * @constructor
 */
var LineGraphView = function () {
    RmodulesView.call(this);
}

/**
 * Inherit RModulesView
 * @type {RmodulesView}
 */
LineGraphView.prototype = new RmodulesView();

/**
 * Correct pointer
 * @type {LineGraphView}
 */
LineGraphView.prototype.constructor = LineGraphView;


/**
 * Get form parameters
 * TODO: Refactor the validation to define validation in FormValidator.js instead here
 * @param form
 * @returns {*}
 */
LineGraphView.prototype.get_form_params = function (form) {

    var dependentVariableEle = Ext.get("divDependentVariable");
    var groupByVariableEle = Ext.get("divGroupByVariable");

    var dependentNodeList = createNodeTypeArrayFromDiv(dependentVariableEle, "setnodetype");
    var groupByNodeList = createNodeTypeArrayFromDiv(groupByVariableEle, "setnodetype");

    //If the user dragged in multiple node types, throw an error.
    if (dependentNodeList.length > 1) {
        Ext.Msg.alert('Error', 'Time/Measurements variable must have same type');
        return;
    }

    if (groupByNodeList.length > 1) {
        Ext.Msg.alert('Error', 'Group concepts variable must have same type');
        return;
    }

    var dependentVariableConceptPath = "";
    var groupByVariableConceptPath = "";

    //If we have multiple items in the Dependent variable box, then we have to flip the graph image.
    var flipImage = false;

    if (dependentVariableEle.dom.childNodes.length > 1) {
        flipImage = true;
    }

    //If the category variable element has children, we need to parse them and concatenate their values.
    if (groupByVariableEle.dom.childNodes[0]) {
        //Loop through the category variables and add them to a comma seperated list.
        for (var nodeIndex = 0; nodeIndex < groupByVariableEle.dom.childNodes.length; nodeIndex++) {
            //If we already have a value, add the seperator.
            if (groupByVariableConceptPath != '') groupByVariableConceptPath += '|'

            //Add the concept path to the string.
            groupByVariableConceptPath += RmodulesView.fetch_concept_path(
                groupByVariableEle.dom.childNodes[nodeIndex])
        }
    }

    //If the category variable element has children, we need to parse them and concatenate their values.
    if (dependentVariableEle.dom.childNodes[0]) {
        //Loop through the category variables and add them to a comma seperated list.
        for (var nodeIndex = 0; nodeIndex < dependentVariableEle.dom.childNodes.length; nodeIndex++) {
            //If we already have a value, add the seperator.
            if (dependentVariableConceptPath != '') dependentVariableConceptPath += '|'

            //Add the concept path to the string.
            dependentVariableConceptPath += RmodulesView.fetch_concept_path(
                dependentVariableEle.dom.childNodes[nodeIndex])
        }
    }

    //Make sure the user entered some items into the variable selection boxes.
    if (dependentVariableConceptPath == '') {
        Ext.Msg.alert('Missing input!', 'Please drag at least one concept into the time/measurements variable box.');
        return;
    }

    var formParams = {
        dependentVariable: dependentVariableConceptPath,
        dependentVariableCategorical: this.isCategorical(dependentNodeList),
        jobType: 'LineGraph',
        plotEvenlySpaced: Ext.get("plotEvenlySpaced").dom.checked,
        projections: [ "rawIntensity" ],
        graphType: Ext.get("graphType").dom.options[Ext.get("graphType").dom.selectedIndex].value,
        groupByVariable: groupByVariableConceptPath,
        groupByVariableCategorical: this.isCategorical(groupByNodeList)
    };

    if (!this.load_high_dimensional_parameters(formParams)) return false;
    this.load_binning_parameters(formParams);

    //Pass in our flag that tells us whether to flip or not.
    formParams["flipImage"] = (flipImage) ? 'TRUE' : 'FALSE';

    return formParams;
}

LineGraphView.prototype.load_high_dimensional_parameters = function (formParams) {

    var _dependentDataType = document.getElementById('dependentVarDataType').value ? document.getElementById('dependentVarDataType').value : 'CLINICAL';
    var dependentGeneList = document.getElementById('dependentPathway').value;

    var _groupByDataType = document.getElementById('groupByVarDataType').value ? document.getElementById('groupByVarDataType').value : 'CLINICAL';
    var _groupByGeneList = document.getElementById('groupByPathway').value;

    formParams["divDependentVariableType"] = _dependentDataType;
    formParams["divDependentVariablePathway"] = dependentGeneList;

    formParams["divGroupByVariableType"] = _groupByDataType;
    formParams["divGroupByVariablePathway"] = _groupByGeneList;

    return true;

}

/**
 * Toggle global binning
 */
LineGraphView.prototype.toggle_binning = function () {
    if ($j("#isBinning").prop('checked') ) {
        GLOBAL.Binning = true;
        $j(".binningDiv").show();
    } else {
        GLOBAL.Binning = false;
        $j(".binningDiv").hide();
    }
}

LineGraphView.prototype.update_manual_binning = function () {

    // Change the ManualBinning flag.
    GLOBAL.ManualBinning = document.getElementById('chkManualBin').checked;

    // Get the type of the variable we are dealing with.
    var variableType = Ext.get('variableType').getValue();

    // Hide both DIVs.
    var divContinuous = Ext.get('divManualBinContinuous');
    var divCategorical = Ext.get('divManualBinCategorical');

    divContinuous.setVisibilityMode(Ext.Element.DISPLAY);
    divCategorical.setVisibilityMode(Ext.Element.DISPLAY);

    divContinuous.hide();
    divCategorical.hide();

    // Show the div with the binning options relevant to our variable type.
    if (document.getElementById('chkManualBin').checked) {
        if (variableType == "Continuous") {
            divContinuous.show();
            divCategorical.hide();
        } else {
            divContinuous.hide();
            divCategorical.show();
            setupCategoricalItemsList("divGroupByVariable","divCategoricalItems");
        }
    }
}

LineGraphView.prototype.manage_bins = function (newNumberOfBins) {

    // This is the row template for a continuous BinningRow.
    var tpl = new Ext.Template(
        '<tr id="binningContinousRow{0}">',
        '<td>Bin {0}</td><td><input type="text" id="txtBin{0}RangeLow" /> - <input type="text" id="txtBin{0}RangeHigh" /></td>',
        '</tr>');
    var tplcat = new Ext.Template(
        '<tr id="binningCategoricalRow{0}">',
        '<td><b>Bin {0}</b><div id="divCategoricalBin{0}" class="manualBinningBin"></div></td>',
        '</tr>');

    // This is the table we add continuous variables to.
    var continuousBinningTable = Ext.get('tblBinContinuous');
    var categoricalBinningTable = Ext.get('tblBinCategorical');

    // For each bin, we add a row to the binning table.
    for (var i = 1; i <= newNumberOfBins; i++) {
        // If the object isn't already on the screen, add it.
        if (!(Ext.get("binningContinousRow" + i))) {
            tpl.append(continuousBinningTable, [ i ]);
        } else {
            Ext.get("binningContinousRow" + i).show();
        }

        // If the object isn't already on the screen, add it-Categorical
        if (!(Ext.get("binningCategoricalRow" + i))) {
            tplcat.append(categoricalBinningTable, [ i ]);
            // Add the drop targets and handler function.
            var bin = Ext.get("divCategoricalBin" + i);
            var dropZone = new Ext.dd.DropTarget(bin, {
                ddGroup : 'makeBin',
                isTarget: true,
                ignoreSelf: false
            });
            // dropZone.notifyEnter = test;
            dropZone.notifyDrop = this.drop_onto_bin; // dont forget to make each
            // dropped
            // node a drag target
        } else {
            Ext.get("binningCategoricalRow" + i).show()
        }
    }

    // If the new number of bins is less than the old, hide the old bins.
    if (newNumberOfBins < GLOBAL.NumberOfBins) {
        // For each bin, we add a row to the binning table.
        for (i = parseInt(newNumberOfBins) + 1; i <= GLOBAL.NumberOfBins; i++) {
            // If the object isn't already on the screen, add it.
            if (Ext.get("binningContinousRow" + i)) {
                Ext.get("binningContinousRow" + i).hide();
            }
            // If the object isn't already on the screen, add it.
            if (Ext.get("binningCategoricalRow" + i)) {
                Ext.get("binningCategoricalRow" + i).hide();
            }
        }
    }

    // Set the global variable to reflect the new bin count.
    GLOBAL.NumberOfBins = newNumberOfBins;
    this.update_manual_binning();
}

LineGraphView.prototype.load_binning_parameters = function (formParams) {

    //These default to FALSE
    formParams["manualBinningGroupBy"] = "FALSE";
    formParams["binningGroupBy"] = "FALSE";

    // Gather the data from the optional binning items, if we had selected to
    // enable binning.
    if (GLOBAL.Binning) {
        // Get the number of bins the user entered.
        var numberOfBins = Ext.get("txtNumberOfBins").getValue()

        // Get the value from the dropdown that specifies the type of
        // binning.
        var binningType = Ext.get("selBinDistribution").getValue()

        // Add these items to our form parameters.
        formParams["binningGroupBy"] = "TRUE";
        formParams["numberOfBinsGroupBy"] = numberOfBins;
        formParams["binDistributionGroupBy"] = binningType;

        // If we are using Manual Binning we need to add the parameters
        // here.
        if (GLOBAL.ManualBinning) {

            // Get a bar separated list of bins and their ranges.
            var binRanges = ""

            // Loop over each row in the HTML table.
            var variableType = Ext.get('variableType').getValue();
            if (variableType == "Continuous") {
                for (i = 1; i <= GLOBAL.NumberOfBins; i++) {
                    binRanges += "bin" + i + ","
                    binRanges += Ext.get('txtBin' + i + 'RangeLow').getValue()
                        + ","
                    binRanges += Ext.get('txtBin' + i + 'RangeHigh').getValue()
                        + "|"
                }
            } else {
                for (i = 1; i <= GLOBAL.NumberOfBins; i++) {
                    binRanges += "bin" + i + "<>"
                    var bin = Ext.get('divCategoricalBin' + i);
                    for (var x = 0; x < bin.dom.childNodes.length; x++) {
                        binRanges+=bin.dom.childNodes[x].getAttribute('conceptdimcode') + "<>"
                    }
                    binRanges=binRanges.substring(0, binRanges.length - 2);
                    binRanges=binRanges+"|";
                }
            }
            formParams["manualBinningGroupBy"] = "TRUE";
            formParams["binRangesGroupBy"] = binRanges.substring(0,binRanges.length - 1);
            formParams["variableTypeGroupBy"] = Ext.get('variableType').getValue();
        }
    }
}

/**
 * Submit the job
 * @param form
 */
LineGraphView.prototype.submit_job = function (form) {

    // get formParams
    var formParams = this.get_form_params(form);

    if (formParams) { // if formParams is not null
        submitJob(formParams);
    }

}

// instantiate line graph instance
var lineGraphView = new LineGraphView();
