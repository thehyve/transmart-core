/**
 * Where everything starts
 * Register drag and drop.
 * Clear out all gobal variables and reset them to blank.
 */
function loadKclustView(){
    kmeansClustering.clear_high_dimensional_input('divIndependentVariable');
    kmeansClustering.register_drag_drop();
}


// constructor
var KMeansClusteringView = function () {
    RmodulesView.call(this);
}

// inherit RmodulesView
KMeansClusteringView.prototype = new RmodulesView();

// correct the pointer
KMeansClusteringView.prototype.constructor = KMeansClusteringView;

// submit analysis job
KMeansClusteringView.prototype.submit_job = function () {
    // get formParams
    var formParams = this.get_form_params();

    if (formParams) { // if formParams is not null
        submitJob(formParams);
    }
}

// get form params
KMeansClusteringView.prototype.get_form_params = function () {
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
        var clusters = inputArray[1].el.value;
        var maxDrawNum = inputArray[2].el.value;
        var imageWidth = inputArray[3].el.value;
        var imageHeight = inputArray[4].el.value;
        var imagePointSize = inputArray[5].el.value;

        // assign values to form parameters
        formParameters['jobType'] = 'RKClust';
        formParameters['independentVariable'] = inputConceptPathVar;
        formParameters['variablesConceptPaths'] = inputConceptPathVar;
        formParameters['txtClusters'] = clusters;
        formParameters['txtMaxDrawNumber'] = maxDrawNum;
        formParameters['txtImageWidth'] = imageWidth;
        formParameters['txtImageHeight'] = imageHeight;
        formParameters['txtImagePointsize'] = imagePointSize;

        //get analysis constraints
        var constraints_json = this.get_analysis_constraints('RKClust');
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

KMeansClusteringView.prototype.get_inputs = function (form_params) {
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
            "label" : "Number of Clusters",
            "el" : document.getElementById("txtClusters"),
            "validations" : [{type:"REQUIRED"}, {type:"INTEGER", min:1}]
        },
        {
            "label" : "Max Row to Display",
            "el" : document.getElementById("txtMaxDrawNumber"),
            "validations" : [{type:"INTEGER", min:1}]
        },
        {
            "label" : "Image Width",
            "el" : document.getElementById("txtImageWidth"),
            "validations" : [{type:"REQUIRED"}, {type:"INTEGER", min:1, max:9000}]
        },
        {
            "label" : "Image Height",
            "el" : document.getElementById("txtImageHeight"),
            "validations" : [{type:"REQUIRED"}, {type:"INTEGER", min:1, max:9000}]
        },
        {
            "label" : "Image Point Size",
            "el" : document.getElementById("txtImagePointsize"),
            "validations" : [{type:"REQUIRED"}, {type:"INTEGER", min:1, max:100}]
        }
    ];
}

// init heat map view instance
var kmeansClustering = new KMeansClusteringView();
