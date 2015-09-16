/**
 * Where everything starts
 */
function loadLogisticRegressionView(){
    logisticRegressionView.register_drag_drop();
    logisticRegressionView.clear_high_dimensional_input('divIndependentVariable');
    logisticRegressionView.clear_high_dimensional_input('divGroupByVariable');
    logisticRegressionView.toggle_binning();
}

/**
 * Constructor
 * @constructor
 */
var LogisticRegressionView = function () {
    RmodulesView.call(this);
}

/**
 * Inherit RModulesView
 * @type {RmodulesView}
 */
LogisticRegressionView.prototype = new RmodulesView();

/**
 * Correct pointer
 * @type {LineGraphView}
 */
LogisticRegressionView.prototype.constructor = LogisticRegressionView;

/**
 * Toggle global binning
 */
LogisticRegressionView.prototype.toggle_binning = function () {
    if ($j("#isBinning").prop('checked') ) {
        GLOBAL.Binning = true;
        $j(".binningDiv").show();
    } else {
        GLOBAL.Binning = false;
        $j(".binningDiv").hide();
    }
}

/**
 * Get form parameters
 * TODO: Refactor the validation to define validation in FormValidator.js instead here
 * @param form
 * @returns {*}
 */
LogisticRegressionView.prototype.get_form_params = function (form) {

    var dependentVariableConceptCode = readConceptVariables("divDependentVariable");
    var independentVariableConceptCode = readConceptVariables("divIndependentVariable");
    var groupByVariableConceptCode = readConceptVariables("divGroupByVariable");
    var variablesConceptCode = independentVariableConceptCode+"|"+groupByVariableConceptCode;

    var formParams = {
        jobType: 'LogisticRegression',
        variablesConceptPaths: variablesConceptCode,
        dependentVariable: dependentVariableConceptCode,
        independentVariable: independentVariableConceptCode,
        groupByVariable: groupByVariableConceptCode
    };

///////////////////////////////////////  VALIDATION

    if(independentVariableConceptCode == '')
    {
        Ext.Msg.alert('Missing input!', 'Please drag one concept into the Independent Variable box.');
        return;
    }

    var groupByVariableEle = Ext.get("divGroupByVariable");
    var independentVariableEle = Ext.get("divIndependentVariable");

    //This will tell us the type of nodes drag into Probability box
    var categoryNodeList = createNodeTypeArrayFromDiv(groupByVariableEle,"setnodetype")
    var numericNodeList = createNodeTypeArrayFromDiv(independentVariableEle,"setnodetype")

    //Across Trial/Navigate by study validation.
    //This will tell us which table the nodes came from. This is important because it tells us if they are modifier
    //nodes or regular concept codes. We use this information for validation and for passing to the jobs functions.
    var categoryNodeType = createNodeTypeArrayFromDiv(groupByVariableEle,"concepttablename")
    var numericNodeType = createNodeTypeArrayFromDiv(independentVariableEle,"concepttablename")

    if (categoryNodeType.length > 1) {
        Ext.Msg.alert('Wrong input', 'The Category input box has nodes from both the \'Navigate By Study\' tree ' +
            'and the \'Across Trial\' tree. Please only use nodes of the same type. ');
        return;
    }
    //If we are not a numeric leaf node, than
    //Make sure user entered a group and a concept

    if (categoryNodeList.length > 1) {
        Ext.Msg.alert('Wrong input', 'You may only drag nodes of the same type (Continuous,Categorical) into ' +
            'the input box. The Probability Concepts input box has multiple types.');
        return;
    }

    //Combine the different arrays so we can make sure the type matches across all input boxes.
    var finalNodeType = []

    //This is what we use to determine if we are running a modifier_cd analysis or a concept_cd analysis.
    var codeType

    if (categoryNodeType[0] && categoryNodeType[0] != "null") finalNodeType.push(categoryNodeType[0])

    //Distinct this final list.
    finalNodeType = finalNodeType.unique()

    if (finalNodeType.length > 1) {
        Ext.Msg.alert('Wrong input', 'You have selected inputs from different ontology trees, please only ' +
            'select nodes from the \'Navigate By Study\' or \'Across Trial\' tree.');
        return;
    }

    if ((categoryNodeList[0] == 'valueicon' || categoryNodeList[0] == 'hleaficon') && (groupByVariableConceptCode.indexOf("|") != -1)) {
        Ext.Msg.alert('Wrong input', 'For continuous data, you may only drag one node into the input boxes and enabe binning to define two outcomes. ' +
            'The Outcome variable input box has multiple nodes.');
        return;
    }

    if ((numericNodeList[0] == 'valueicon' || numericNodeList[0] == 'hleaficon') && (independentVariableConceptCode.indexOf("|") != -1)) {
        Ext.Msg.alert('Wrong input', 'For continuous data, you may only drag one node into the input boxes. ' +
            'The Independent variable input box has multiple nodes.');
        return;
    }

    //If its categorical value than make sure you have atleast 2 values
    if (groupByVariableConceptCode == '' || (  categoryNodeList[0] != 'valueicon' && categoryNodeList[0] != 'hleaficon' && groupByVariableEle.dom.childNodes.length < 2)) {
        Ext.Msg.alert('Missing input!', 'If categorical concept, than please drag at least two categorical ' +
            'concept into the Outcome variable input box.');
        return;
    }

    //If its categorical value and its more than 2 values, than make sure they are binned manually
    if (categoryNodeList[0] != 'valueicon' && groupByVariableEle.dom.childNodes.length > 2 && !GLOBAL.Binning) {
        Ext.Msg.alert('Wrong input!', 'For more than 2 categorical concepts,  please enable binning and use manual ' +
            'binning to group the concepts into 2 groups');
        return;
    }

    //If its continuous value, than make sure they are binned
    if (categoryNodeList[0] == 'valueicon' && !GLOBAL.Binning) {
        Ext.Msg.alert('Wrong input!', 'For continuous data,  please enable binning and bin the concepts into 2 groups');
        return;
    }

    //If binning is enabled and we try to bin a categorical value as a continuous, throw an error.
    if ((categoryNodeList[0] != 'valueicon' && categoryNodeList[0] != 'hleaficon') && GLOBAL.Binning && Ext.get('variableType').getValue() == 'Continuous') {
        Ext.Msg.alert('Wrong input', 'There is a categorical input in the Category box, but you are trying to ' +
            'bin it as if it was continuous. Please alter your binning options or the concept in the Category box.');
        return;
    }

    //If binning is enabled, we are doing categorical and the manual binning checkbox is not checked, alert the user.
    if (GLOBAL.Binning && Ext.get('variableType').getValue() != 'Continuous' && !GLOBAL.ManualBinning) {
        Ext.Msg.alert('Wrong input', 'You must enable manual binning when binning a categorical variable.');
        return;
    }
    //If binning is enabled and the user is trying to categorically bin a continuous variable, alert them.
    if (GLOBAL.Binning && Ext.get('variableType').getValue() != 'Continuous' && (categoryNodeList[0] == 'valueicon')) {
        Ext.Msg.alert('Wrong input', 'You cannot use categorical binning with a continuous variable. Please alter ' +
            'your binning options or the concept in the Probablity input  box.');
        return;
    }
    if(!this.load_high_dimensional_parameters(formParams)) return false;
    this.load_binning_parameters(formParams);

    //------------------------------------
    //More Validation
    //------------------------------------
    //If the user dragged in a high dim node, but didn't enter the High Dim Screen, throw an error.
    if(categoryNodeList[0] == 'hleaficon' && formParams["divGroupByVariableType"] == "CLINICAL")
    {
        Ext.Msg.alert('Wrong input', 'You dragged a High Dimensional Data node into the outcome variable box but ' +
            'did not select any filters. Please click the "High Dimensional Data" button and select filters. ' +
            'Apply the filters by clicking "Apply Selections".');
        return;
    }
    if(numericNodeList[0] == 'hleaficon' && formParams["divIndependentVariableType"] == "CLINICAL")
    {
        Ext.Msg.alert('Wrong input', 'You dragged a High Dimensional Data node into the independent variable ' +
            'box but did not select any filters. Please click the "High Dimensional Data" button and select ' +
            'filters. Apply the filters by clicking "Apply Selections".');
        return;
    }
    //------------------------------------


    return formParams;
}

/**
 * Update form if manual binning is selected.
 */
LogisticRegressionView.prototype.update_manual_binning = function () {
    // Change the ManualBinning flag.
    GLOBAL.ManualBinning = document.getElementById('chkManualBin').checked;

    // Get the type of the variable we are dealing with.
    variableType = Ext.get('variableType').getValue();

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
            setupCategoricalItemsList("divGroupByVariable", "divCategoricalItems");
        }
    }
}

LogisticRegressionView.prototype.manage_bins = function (newNumberOfBins) {

    // This is the row template for a continousBinningRow.
    var tpl = new Ext.Template(
        '<tr id="binningContinousRow{0}">',
        '<td>Bin {0}</td><td><input type="text" id="txtBin{0}RangeLow" title="Low Range" /> - <input type="text" id="txtBin{0}RangeHigh" title="High Range" /></td>',
        '</tr>');
    var tplcat = new Ext.Template(
        '<tr id="binningCategoricalRow{0}">',
        '<td><b>Bin {0}</b><div id="divCategoricalBin{0}" class="queryGroupIncludeSmall"></div></td>',
        '</tr>');

    // This is the table we add continuous variables to.
    continuousBinningTable = Ext.get('tblBinContinuous');
    categoricalBinningTable = Ext.get('tblBinCategorical');
    // Clear all old rows out of the table.

    // For each bin, we add a row to the binning table.
    for (i = 1; i <= newNumberOfBins; i++) {
        // If the object isn't already on the screen, add it.
        if (!(Ext.get("binningContinousRow" + i))) {
            tpl.append(continuousBinningTable, [ i ]);
        } else {
            Ext.get("binningContinousRow" + i).show()
        }

        // If the object isn't already on the screen, add it-Categorical
        if (!(Ext.get("binningCategoricalRow" + i))) {
            tplcat.append(categoricalBinningTable, [ i ]);
            // Add the drop targets and handler function.
            var bin = Ext.get("divCategoricalBin" + i);
            var dragZone = new Ext.dd.DragZone(bin, {
                ddGroup : 'makeBin',
                isTarget: true,
                ignoreSelf: false
            });
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

LogisticRegressionView.prototype.load_high_dimensional_parameters = function (formParams) {

    var _independentGeneList = document.getElementById('independentPathway').value;
    var _independentDataType = document.getElementById('independentVarDataType').value ?
        document.getElementById('independentVarDataType').value : 'CLINICAL';

    formParams["divIndependentVariableType"]     = _independentDataType;
    formParams["divIndependentVariablePathway"]  = _independentGeneList;

    var _groupByGeneList = document.getElementById('groupByPathway').value;
    var _groupByDataType = document.getElementById('groupByVarDataType').value ?
        document.getElementById('groupByVarDataType').value : 'CLINICAL';

    formParams["divGroupByVariableType"]         = _groupByDataType;
    formParams["divGroupByVariablePathway"]      = _groupByGeneList;

    return true;
}

LogisticRegressionView.prototype.load_binning_parameters = function (formParams) {
    //These default to FALSE
    formParams["binning"] = "FALSE";
    formParams["manualBinning"] = "FALSE";

    // Gather the data from the optional binning items, if we had selected to
    // enable binning.
    if (GLOBAL.Binning) {
        // Get the number of bins the user entered.
        var numberOfBins = 2 //Ext.get("txtNumberOfBins").getValue()

        // Get the value from the dropdown that specifies the type of
        // binning.
        var binningType = Ext.get("selBinDistribution").getValue()

        //Get the value from the dropdown that tells us which variable to bin.
        var binningVariable = "IND" //Ext.get("selBinVariableSelection").getValue()

        // Add these items to our form parameters.
        formParams["binning"] = "TRUE";
        formParams["numberOfBins"] = numberOfBins;
        formParams["binDistribution"] = binningType;
        formParams["binVariable"] = binningVariable;

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
                    for (x = 0; x < bin.dom.childNodes.length; x++) {
                        binRanges+=bin.dom.childNodes[x].getAttribute('conceptdimcode') + "<>"
                    }
                    binRanges=binRanges.substring(0, binRanges.length - 2);
                    binRanges=binRanges+"|";
                }
            }
            formParams["manualBinning"] = "TRUE";
            formParams["binRanges"] = binRanges.substring(0,binRanges.length - 1);
            formParams["variableType"] = Ext.get('variableType').getValue();
        }
    }
}

/**
 * Submit the job
 * @param form
 */
LogisticRegressionView.prototype.submit_job = function (form) {

    // get formParams
    var formParams = this.get_form_params(form);

    if (formParams) { // if formParams is not null
        submitJob(formParams);
    }

}

// instantiate line graph instance
var logisticRegressionView = new LogisticRegressionView();
