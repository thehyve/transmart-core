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
    geneprintView.boilerplate = MainBoilerplate(oncoprint, OncoprintUtils);
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
        // At this point, we will have the node details loaded,
        // so we'll check for matching data types
        var validDataTypes = ['mrna', 'protein', 'acgh', 'vcf'];
        for (var key in highDimensionalData.data) {
            // yes, the keys are the data types
            if (validDataTypes.indexOf(key) == -1) {
                Ext.MessageBox.show({
                    title: "Validation Error",
                    msg: "One of the data types is " + key + ", but Geneprint only supports [" + validDataTypes.join(", ") + "]",
                    buttons: Ext.MessageBox.OK,
                    icon: Ext.MessageBox.ERROR
                });
                return;
            }
        }

        // Check if gene(s) have been selected; this is required for geneprint
        if (!GLOBAL.CurrentPathway) {
            Ext.MessageBox.show({
                title: "Validation Error",
                msg: "Please select genes for the high dimensional data",
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.ERROR
            });
            return;
        }

        // get formParams
        var formParams = job.get_form_params();

        if (formParams) { // if formParams is not null
            //submitJob(formParams);

            // Set up progress dialog
            $j('.runAnalysisBtn')[0].disabled = true;
            job.showProgress($j('#dataAssociationBody'), true);

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

    // Fetch the node details for the high dimensional data
    var divId = 'divIndependentVariable';
    runAllQueriesForSubsetId(function () {
        highDimensionalData.fetchNodeDetails(divId, function( result ) {
            highDimensionalData.data = JSON.parse(result.responseText);
            highDimensionalData.populate_data();
            actualSubmit();
        });
    }, divId);
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
        constraints_json['mrnaThreshold'] = inputArray[1].el.value;
        constraints_json['proteinThreshold'] = inputArray[2].el.value;

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
        },
        {
            "label" : "mRNA expression z-score threshold",
            "el" : document.getElementById("txtMrnaThreshold"),
            "validations" : [{type:"DECIMAL", min:0.0}]
        },
        {
            "label" : "Protein expression z-score threshold",
            "el" : document.getElementById("txtProteinThreshold"),
            "validations" : [{type:"DECIMAL", min:0.0}]
        }
    ];
}

GeneprintView.prototype.showProgress = function(parentElem, noTitleBar) {
    destroyWorkflowStatus();

    var maskDiv = $j(document.createElement('div')).attr({id: 'mask'});
    maskDiv.css('z-index', 10000);
    $j('#dataAssociationBody').append(maskDiv);


    //Add new modal-dialog
    var progressBarDiv = $j(document.createElement('div')).attr({id: 'progress-bar'});
    var progressStatusSpan = $j(document.createElement('span')).attr({id: 'progress-status'});
    progressStatusSpan.html('Running analysis');
    var progressStatusImg = $j(document.createElement('div')).attr({id: 'progress-img'});
    var progressTextDiv = $j(document.createElement('div')).attr({id: 'progress-text'});
    progressTextDiv.append(progressStatusImg);
    progressTextDiv.append(progressStatusSpan);

    var modalDialogDiv = $j(document.createElement('div')).attr({id: 'dialog-modal'});
    modalDialogDiv.append(progressBarDiv);
    modalDialogDiv.append(progressTextDiv);

    parentElem.append(modalDialogDiv);
    $j("#progress-img").attr('class', 'progress-spinner');

    $j("#mask").fadeTo(500, 0.25);

    var d = $j("#dialog-modal").dialog({
        height: 130,
        minHeight: 130,
        maxHeight: 130,
        width: 300,
        minWidth: 250,
        maxWidth: 350,
        closeOnEscape: false,
        show: { effect: 'drop', direction: "up" },
        hide: { effect: 'fade', duration: 200 },
        dialogClass: 'dialog-modal',
        title: 'Workflow Status',
        position: {
            my: 'left top',
            at: 'center',
            of: parentElem
        },
        buttons: {
            "Stop Analysis": this.cancelJob
        },
        //To hide the header of the dialog
        create: function (event, ui) {
            if (noTitleBar) $j(".ui-widget-header", $(ui)).hide();
        },
        close: function (event, ui) {
            $j("#mask").hide();
            $j("#mask").remove();
            $j("#dialog-modal").dialog('destroy');
            //$j('#mask').remove();
        },
        zIndex: 10001,
        //modal: true,
        autoOpen: false
    });
    d.parent('.ui-dialog').appendTo($j('#dataAssociationBody'));
    $j("#dialog-modal").dialog('open');

    $j("#progress-bar").progressbar({
        value: 5
    });
}

GeneprintView.prototype.cancelJob = function() {
    geneprintView.boilerplate.xhr.abort();
}

// init geneprint view instance
var geneprintView = new GeneprintView();
