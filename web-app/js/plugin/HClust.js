/**
 * Register drag and drop.
 * Clear out all global variables and reset them to blank.
 */
function loadHclustView(){
    hierarchicalClusteringView.clear_high_dimensional_input('divIndependentVariable');
    hierarchicalClusteringView.register_drag_drop();
}

// constructor
var HierarchicalClusteringView = function () {
    RmodulesView.call(this);
}

// inherit RmodulesView
HierarchicalClusteringView.prototype = new RmodulesView();

// correct the pointer
HierarchicalClusteringView.prototype.constructor = HierarchicalClusteringView;

// submit analysis job
HierarchicalClusteringView.prototype.submit_job = function () {

    // get formParams
    var formParams = this.get_form_params();

    if (formParams) { // if formParams is not null
        submitJob(formParams);
    }

}


// get form params
HierarchicalClusteringView.prototype.get_form_params = function () {
    var formParameters = {}; // init

    //Use a common function to load the High Dimensional Data params.
    loadCommonHighDimFormObjects(formParameters, "divIndependentVariable");

    // instantiate input elements object with their corresponding validations
    var inputArray = this.get_inputs(formParameters);

    // define the validator for this form
    var formValidator = new FormValidator(inputArray);

    if (formValidator.validateInputForm()) { // if input files satisfy the validations

        // get values
        var inputConceptPathVar = readConceptVariables("divIndependentVariable");
        var maxDrawNum = inputArray[1].el.value;
        var doClusterRows = inputArray[2].el.checked;
        var doClusterColumns = inputArray[3].el.checked;

        // assign values to form parameters
        formParameters['jobType'] = 'RHClust';
        formParameters['independentVariable'] = inputConceptPathVar;
        formParameters['variablesConceptPaths'] = inputConceptPathVar;
        formParameters['txtMaxDrawNumber'] = maxDrawNum;
        formParameters['doClusterRows'] = doClusterRows;
        formParameters['doClusterColumns'] = doClusterColumns;

        //get analysis constraints
        var constraints_json = this.get_analysis_constraints('RHClust');
        constraints_json['projections'] = ["zscore"];

        formParameters['analysisConstraints'] = JSON.stringify(constraints_json);

    } else { // something is not correct in the validation
        // empty form parameters
        formParameters = null;
        // display the error message
        formValidator.display_errors();
    }

    return formParameters;
}

HierarchicalClusteringView.prototype.get_inputs = function (form_params) {
    return  [
        {
            "label" : "High Dimensional Data",
            "el" : Ext.get("divIndependentVariable"),
            "validations" : [
                {type:"REQUIRED"},
                {
                    type:"HIGH_DIMENSIONAL",
                    high_dimensional_type:form_params["divIndependentVariableType"],
                    high_dimensional_pathway:form_params["divIndependentVariablePathway"]
                }
            ]
        },
        {
            "label" : "Max Row to Display",
            "el" : document.getElementById("txtMaxDrawNumber"),
            "validations" : [{type:"INTEGER", min:1}]
        },
        {
            "label" : "Do cluster rows",
            "el" : document.getElementById("chkClusterRows"),
            "validations" : []
        },
        {
            "label" : "Do cluster columns",
            "el" : document.getElementById("chkClusterColumns"),
            "validations" : []
        }
    ];
}


// init heat map view instance
var hierarchicalClusteringView = new HierarchicalClusteringView();
