/**
 * Where everything starts:
 *  - Register drag and drop.
 *  - Clear out all gobal variables and reset them to blank.
 */
function loadPcaView(){
    pcaView.clear_high_dimensional_input('divIndependentVariable');
    pcaView.register_drag_drop();
}

// constructor
var PCAView = function () {
    RmodulesView.call(this);
}

// inherit RmodulesView
PCAView.prototype = new RmodulesView();

// correct the pointer
PCAView.prototype.constructor = PCAView;

// submit analysis job
PCAView.prototype.submit_job = function () {

    // get formParams
    var formParams = this.get_form_params();

    if (formParams) { // if formParams is not null
        submitJob(formParams);
    }
}

// get form params
PCAView.prototype.get_form_params = function () {
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
        var doUseExperimentAsVariable = inputArray[1].el.checked;

        // assign values to form parameters
        formParameters['jobType'] = 'PCA';
        formParameters['independentVariable'] = inputConceptPathVar;
        formParameters['variablesConceptPaths'] = inputConceptPathVar;
        formParameters['doUseExperimentAsVariable'] = doUseExperimentAsVariable;

        //formParameters['analysisConstraints'] = JSON.stringify(this.get_analysis_constraints('PCA'));

        //get analysis constraints
        var constraints_json = this.get_analysis_constraints('PCA');
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

PCAView.prototype.get_inputs = function (form_params) {
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
            "label" : "Do Use Experiment As Variable",
            "el" : document.getElementById("chkUseExperimentAsVariable"),
            "validations" : []
        }
    ];
}

// init view instance
var pcaView = new PCAView();
