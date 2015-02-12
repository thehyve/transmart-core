/**
 * Where everything starts
 * - Register drag and drop.
 * - Clear out all global variables and reset them to blank.
 */
function loadCorrelationAnalysisView(){
    correlationAnalysisView.clear_high_dimensional_input('divVariables');
    correlationAnalysisView.register_drag_drop();
}

// constructor
var CorrelationAnalysisView = function () {
    RmodulesView.call(this);
}

// inherit RmodulesView
CorrelationAnalysisView.prototype = new RmodulesView();

// correct the pointer
CorrelationAnalysisView.prototype.constructor = CorrelationAnalysisView;

// submit analysis job
CorrelationAnalysisView.prototype.submit_job = function (form) {
    var variablesConceptCode = readConceptVariables("divVariables");

    var formParams = {
        jobType:'correlationAnalysis',
        variablesConceptPaths:variablesConceptCode,
        correlationBy:"variable",
        correlationType:form.correlationType.value
    };

    var variableEle = Ext.get("divVariables");

    //If the list of concepts we are running the analysis on is empty, alert the user.
    if(variablesConceptCode == '' || (variableEle.dom.childNodes.length < 2))
    {
        Ext.Msg.alert('Missing input!', 'Please drag at least two concepts into the variables box.');
        return;
    }

    submitJob(formParams);
}

// init heat map view instance
var correlationAnalysisView = new CorrelationAnalysisView();
