/**
 * Where everything starts
 */
function loadLineGraphView(){
    lineGraphView.register_drag_drop();
    lineGraphView.clear_high_dimensional_input('divGroupByVariable');
    lineGraphView.clear_high_dimensional_input('divDependentVariable');
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
    var independentVariableEle = Ext.get("divGroupByVariable");

    var dependentNodeList = createNodeTypeArrayFromDiv(dependentVariableEle,"setnodetype")
    var independentNodeList = createNodeTypeArrayFromDiv(independentVariableEle,"setnodetype")

    //If the user dragged in multiple node types, throw an error.
    if (dependentNodeList.length > 1) {
        Ext.Msg.alert('Error', 'Dependent variable must have same type');
        return;
    }

    if (independentNodeList.length > 1) {
        Ext.Msg.alert('Error', 'Independent variable must have same type');
        return;
    }

    /**
     * To check if node is categorical or not
     * @param nodeTypes
     * @returns {boolean}
     * @private
     */
    var _isCategorical = function (nodeTypes) {
        return (nodeTypes[0] == "null") ? true : false;
    } //


    var dependentVariableConceptCode = "";
    var independentVariableConceptCode = "";

    //If we have multiple items in the Dependent variable box, then we have to flip the graph image.
    var flipImage = false;

    if ((dependentVariableEle.dom.childNodes.length > 1) {
        flipImage = true;
    }

    //If the category variable element has children, we need to parse them and concatenate their values.
    if (independentVariableEle.dom.childNodes[0]) {
        //Loop through the category variables and add them to a comma seperated list.
        for (nodeIndex = 0; nodeIndex < independentVariableEle.dom.childNodes.length; nodeIndex++) {
            //If we already have a value, add the seperator.
            if (independentVariableConceptCode != '') independentVariableConceptCode += '|'

            //Add the concept path to the string.
            independentVariableConceptCode += getQuerySummaryItem(independentVariableEle.dom.childNodes[nodeIndex]).trim()
        }
    }

    //If the category variable element has children, we need to parse them and concatenate their values.
    if (dependentVariableEle.dom.childNodes[0]) {
        //Loop through the category variables and add them to a comma seperated list.
        for (nodeIndex = 0; nodeIndex < dependentVariableEle.dom.childNodes.length; nodeIndex++) {
            //If we already have a value, add the seperator.
            if (dependentVariableConceptCode != '') dependentVariableConceptCode += '|'

            //Add the concept path to the string.
            dependentVariableConceptCode += getQuerySummaryItem(dependentVariableEle.dom.childNodes[nodeIndex]).trim()
        }
    }

    //Make sure the user entered some items into the variable selection boxes.
    if (dependentVariableConceptCode == '') {
        Ext.Msg.alert('Missing input!', 'Please drag at least one concept into the dependent variable box.');
        return;
    }

    if (independentVariableConceptCode == '') {
        Ext.Msg.alert('Missing input!', 'Please drag at least one concept into the independent variable box.');
        return;
    }

    var variablesConceptCode = dependentVariableConceptCode + "|" + independentVariableConceptCode;

    var formParams = {
        dependentVariable: dependentVariableConceptCode,
        dependentVariableCategorical: _isCategorical(dependentNodeList),
        independentVariable: independentVariableConceptCode,
        independentVariableCategorical: _isCategorical(independentNodeList),
        jobType: 'LineGraph',
        variablesConceptPaths: variablesConceptCode
    };

    if (!highDimensionalData.load_parameters(formParams)) return false;

    //Pass in our flag that tells us whether to flip or not.
    formParams["flipImage"] = (flipImage) ? 'TRUE' : 'FALSE';

    return formParams;
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

// instantiate table fisher instance
var lineGraphView = new LineGraphView();
