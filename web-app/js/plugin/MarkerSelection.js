/**
 * Register drag and drop.
 * Clear out all gobal variables and reset them to blank.
 */
function loadMarkerSelectionView(){
    markerSelectionView.clear_high_dimensional_input('divIndependentVariable');
    markerSelectionView.register_drag_drop();
}

// constructor
var MarkerSelectionView = function () {
    RmodulesView.call(this);
}

// inherit RmodulesView
MarkerSelectionView.prototype = new RmodulesView();

// correct the pointer
MarkerSelectionView.prototype.constructor = MarkerSelectionView;

// submit analysis job
MarkerSelectionView.prototype.submit_job = function () {
    var job = this;

    var actualSubmit = function() {
        // get formParams
        var formParams = job.get_form_params();

        if (formParams) { // if formParams is not null
            submitJob(formParams);
        }
    }

    // Check whether we have the node details for the HD node already
    // If not, we should fetch them first
    if (typeof GLOBAL.HighDimDataType !== "undefined" && GLOBAL.HighDimDataType) {
        actualSubmit();
    } else {
        var divId = 'divIndependentVariable';
        runAllQueriesForSubsetId(function () {
            highDimensionalData.fetchNodeDetails(divId, function( result ) {
                highDimensionalData.data = JSON.parse(result.responseText);
                highDimensionalData.populate_data();
                actualSubmit();
            });
        }, divId);
    }
}

// get form params
MarkerSelectionView.prototype.get_form_params = function () {

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
        var numOfMarkers = inputArray[1].el.value;
        var doGroupBySubject = inputArray[3].el.checked;

        // assign values to form parameters
        formParameters['jobType'] = 'MarkerSelection';
        formParameters['independentVariable'] = inputConceptPathVar;
        formParameters['variablesConceptPaths'] = inputConceptPathVar;
        formParameters['txtNumberOfMarkers'] = numOfMarkers;
        formParameters['doGroupBySubject'] = doGroupBySubject;

        // get analysis constraints
        var constraints_json = this.get_analysis_constraints('MarkerSelection');
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

MarkerSelectionView.prototype.get_inputs = function (form_params) {
    return [
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
            "label" : "Number of Markers",
            "el" : document.getElementById("txtNumberOfMarkers"),
            "validations" : [{type:"REQUIRED"}, {type:"INTEGER", min:1}]
        },
        {
            "label" : "Subsets",
            "el" : null,
            "value" : [GLOBAL.CurrentSubsetIDs[1], GLOBAL.CurrentSubsetIDs[2]],
            "validations" : [{type:"MIN_TWO_SUBSETS"}]
        },
        {
            "label" : "Do Group by Subject",
            "el" : document.getElementById("chkGroupBySubject"),
            "validations" : []
        }
    ];
}

function loadMarkerSelectionOutput() {
  $j("#markerSelectionTable").tablesorter();
}

// init heat map view instance
var markerSelectionView = new MarkerSelectionView();
