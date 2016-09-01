/**
 * Register drag and drop.
 * Clear out all global variables and reset them to blank.
 */
function loadScatterPlotView(){
    scatterPlotView.clear_high_dimensional_input('divIndependentVariable');
    scatterPlotView.clear_high_dimensional_input('divDependentVariable');
    scatterPlotView.register_drag_drop();
}

// constructor
var ScatterPlotView = function () {
    RmodulesView.call(this);
}

// inherit RmodulesView
ScatterPlotView.prototype = new RmodulesView();

// correct the pointer
ScatterPlotView.prototype.constructor = ScatterPlotView;

// submit analysis job
ScatterPlotView.prototype.submit_job = function (form) {

    // get formParams
    var formParams = this.get_form_params(form);

    if (formParams) { // if formParams is not null
        submitJob(formParams);
    }
}

// get form params
ScatterPlotView.prototype.get_form_params = function (form) {

    var dependentVariableConceptCode = readConceptVariables("divDependentVariable");
    var independentVariableConceptCode = readConceptVariables("divIndependentVariable");

    //------------------------------------
    //Validation
    //------------------------------------
    //Make sure the user entered some items into the variable selection boxes.
    if(dependentVariableConceptCode == '' && independentVariableConceptCode == '')
    {
        Ext.Msg.alert('Missing input', 'Please drag at least one concept into the independent variable and ' +
            'dependent variable boxes.');
        return;
    }
    if(dependentVariableConceptCode == '')
    {
        Ext.Msg.alert('Missing input', 'Please drag at least one concept into the dependent variable box.');
        return;
    }
    if(independentVariableConceptCode == '')
    {
        Ext.Msg.alert('Missing input', 'Please drag at least one concept into the independent variable box.');
        return;
    }

    //Loop through the dependent variable box and find the the of nodes in the box.
    var dependentVariableEle = Ext.get("divDependentVariable");
    var independentVariableEle = Ext.get("divIndependentVariable");

    var dependentNodeList = createNodeTypeArrayFromDiv(dependentVariableEle,"setnodetype")
    var independentNodeList = createNodeTypeArrayFromDiv(independentVariableEle,"setnodetype")

    //If the user dragged in multiple node types, throw an error.
    if(dependentNodeList.length > 1)
    {
        Ext.Msg.alert('Wrong input', 'You may only drag nodes of the same type (Continuous,Categorical,' +
            'High Dimensional) into the input box. The Dependent input box has multiple types.');
        return;
    }

    if(independentNodeList.length > 1)
    {
        Ext.Msg.alert('Wrong input', 'You may only drag nodes of the same type (Continuous,Categorical,' +
            'High Dimensional) into the input box. The Independent input box has multiple types.');
        return;
    }

    //For the valueicon and hleaficon nodes, you can only put one in a given input box.
    if((this.isNumerical(dependentNodeList) || this.isHd(dependentNodeList)) &&
        (dependentVariableConceptCode.indexOf("|") != -1)) {
        Ext.Msg.alert('Wrong input', 'For continuous and high dimensional data, you may only drag one node into the ' +
            'input boxes. The Dependent input box has multiple nodes.');
        return;
    }

    if((this.isNumerical(independentNodeList) || this.isHd(independentNodeList)) &&
        (independentVariableConceptCode.indexOf("|") != -1)) {
        Ext.Msg.alert('Wrong input', 'For continuous and high dimensional data, you may only drag one node into the ' +
            'input boxes. The Independent input box has multiple nodes.');
        return;
    }

    //Nodes will be either 'hleaficon' or 'valueicon'.
    //Scatter plot requires 2 continuous variables.
    var depVariableType = "";
    var indVariableType = "";

    //If there is a categorical variable in either box (This means either of the lists are empty)
    if(this.isCategorical(dependentNodeList)) depVariableType = "CAT";
    if(this.isCategorical(independentNodeList)) indVariableType = "CAT";

    //If we have a value icon node, or a high dim that isn't SNP genotype, it is continuous.
    if((this.isNumerical(dependentNodeList) || (this.isHd(dependentNodeList) &&
        !(window['divDependentVariableSNPType'] == "Genotype" && window['divDependentVariablemarkerType'] == 'SNP'))))
        depVariableType = "CON";

    if((this.isNumerical(independentNodeList) || (this.isHd(independentNodeList) &&
        !(window['divIndependentVariableSNPType'] == "Genotype" && window['divIndependentVariablemarkerType'] == 'SNP'))))
        indVariableType = "CON";

    //If we don't have two continuous variables, throw an error.
    if(!(depVariableType=="CON"))
    {
        Ext.Msg.alert('Wrong input', 'Scatter plot requires 2 continuous variables and the dependent ' +
            'variable is not continuous.');
        return;
    }

    if(!(indVariableType=="CON"))
    {
        Ext.Msg.alert('Wrong input', 'Scatter plot requires 2 continuous variables and the independent ' +
            'variable is not continuous.');
        return;
    }

    //------------------------------------

    var logX = form.divIndependentVariableLog10.checked;

    var variablesConceptCode = dependentVariableConceptCode+"|"+independentVariableConceptCode;

    var formParams = {
        dependentVariable:              dependentVariableConceptCode,
        independentVariable:            independentVariableConceptCode,
        variablesConceptPaths:          variablesConceptCode,
        divIndependentVariableLog10:    logX,
        jobType:                        'ScatterPlot'
    }

    if(!highDimensionalData.load_parameters(formParams)) return false;


    //------------------------------------
    //More Validation
    //------------------------------------
    //If the user dragged in a high dim node, but didn't enter the High Dim Screen, throw an error.
    if(this.isHd(dependentNodeList) && formParams["divDependentVariableType"] == "CLINICAL")
    {
        Ext.Msg.alert('Wrong input', 'You dragged a High Dimensional Data node into the dependent variable box but ' +
            'did not select any filters. Please click the "High Dimensional Data" button and select filters. ' +
            'Apply the filters by clicking "Apply Selections".');
        return;
    }
    if(this.isHd(independentNodeList) && formParams["divIndependentVariableType"] == "CLINICAL")
    {
        Ext.Msg.alert('Wrong input', 'You dragged a High Dimensional Data node into the independent variable ' +
            'box but did not select any filters. Please click the "High Dimensional Data" button and select ' +
            'filters. Apply the filters by clicking "Apply Selections".');
        return;
    }
    //------------------------------------

    return formParams;
}

// init heat map view instance
var scatterPlotView = new ScatterPlotView();