function loadHistogramView() {
    histogramView.clear_high_dimensional_input('divDataNode');
    histogramView.register_drag_drop();
}

// constructor
var HistogramView = function () {
    RmodulesView.call(this);
}

// inherit RmodulesView
HistogramView.prototype = new RmodulesView();

// correct the pointer
HistogramView.prototype.constructor = HistogramView;

// submit analysis job
HistogramView.prototype.submit_job = function () {
    var formParams = this.get_form_params();
    submitJob(formParams);
}

// get form params
HistogramView.prototype.get_form_params = function () {
    var formParameters = { jobType: 'Histogram' };

    var variablesConceptCode = readConceptVariables("divDataNode");
    if (variablesConceptCode == '') {
        Ext.Msg.alert('Missing input!', 'Please drag a concept into the variable box.');
        return;
    }
    formParameters['variablesConceptPath'] = variablesConceptCode;

    return formParameters;
}


var histogramView = new HistogramView();


