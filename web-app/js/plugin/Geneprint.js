/**
 * Where everything starts
 * - Register drag and drop.
 * - Clear out all gobal variables and reset them to blank.
 */
function loadGeneprintView(){
    geneprintView.clear_high_dimensional_input('divIndependentVariable');
    geneprintView.register_drag_drop();
}

function loadGeneprintOutput() {

    // Store the currently selected options as global variables;
    window.cancer_study_id_selected = 'coadread_tcga_pub';
    window.case_set_id_selected = 'coadread_tcga_pub_3way_complete';
    window.gene_set_id_selected = 'user-defined-list';
    window.tab_index = 'tab_visualize';
    window.zscore_threshold = '2.0';
    window.rppa_score_threshold = '2.0';

    //  Store the currently selected genomic profiles within an associative array
    window.genomic_profile_id_selected = new Array();
    window.genomic_profile_id_selected['coadread_tcga_pub_mutations']=1;
    window.genomic_profile_id_selected['coadread_tcga_pub_mrna_median_Zscores']=1;
    window.genomic_profile_id_selected['coadread_tcga_pub_gistic']=1;

    window.PortalGlobals = {
        getCases: function() { return ''; } // list of queried case ids
    };

    var oncoprint = OncoprintCore(OncoprintUtils, MemoSort);
    MainBoilerplate(oncoprint, OncoprintUtils);
}

// constructor
var GeneprintView = function () {
    RmodulesView.call(this);
}

// inherit RmodulesView
GeneprintView.prototype = new RmodulesView();

// correct the pointer
GeneprintView.prototype.constructor = GeneprintView;

// submit analysis job
GeneprintView.prototype.submit_job = function () {
    var job = this;

    var actualSubmit = function() {
        // get formParams
        var formParams = job.get_form_params();

        if (formParams) { // if formParams is not null
            //submitJob(formParams);

            // Set up progress dialog
            $('.runAnalysisBtn')[0].disabled = true;
            createWorkflowStatus($j('#dataAssociationBody'), true);

            window.formParams = formParams;

            // Immediately render the output; the oncoprint javascript
            // will make the calls to the backend directly.
            var url = pageInfo.basePath + "/geneprint/geneprintOut";
            //Set the results DIV to use the URL from the job.
            Ext.get('analysisOutput').load({url: url, callback: loadGeneprintOutput});
            //Set the flag that says we run an analysis so we can warn the user if they navigate away.
            GLOBAL.AnalysisRun = true;

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
GeneprintView.prototype.get_form_params = function () {
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

        // assign values to form parameters
        formParameters['jobType'] = 'Geneprint';
        formParameters['independentVariable'] = inputConceptPathVar;
        formParameters['variablesConceptPaths'] = inputConceptPathVar;

        // get analysis constraints
        var constraints_json = this.get_analysis_constraints('Geneprint');
        constraints_json['projections'] = ["all_data"];

        formParameters['analysisConstraints'] = JSON.stringify(constraints_json);

    } else { // something is not correct in the validation
        // empty form parameters
        formParameters = null;
        // display the error message
        formValidator.display_errors();
    }

    return formParameters;
}

GeneprintView.prototype.get_inputs = function (form_params) {
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
        }
    ];
}

// init geneprint view instance
var geneprintView = new GeneprintView();
