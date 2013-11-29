
var RmodulesView = function () {}

RmodulesView.prototype.clear_high_dimensional_input = function (div) {
    //Clear the drag and drop div.
    var qc = Ext.get(div);

    for (var i = qc.dom.childNodes.length - 1; i >= 0; i--) {
        var child = qc.dom.childNodes[i];
        qc.dom.removeChild(child);
    }
    clearHighDimDataSelections(div);
    clearSummaryDisplay(div);
}

RmodulesView.prototype.register_drag_drop = function () {
    //Set up drag and drop for Dependent and Independent variables on the data association tab.
    //Get the Independent DIV
    var independentDiv = Ext.get("divIndependentVariable");

    dtgI = new Ext.dd.DropTarget(independentDiv, {ddGroup : 'makeQuery'});
    dtgI.notifyDrop =  dropOntoCategorySelection;
}

RmodulesView.prototype.get_analysis_constraints = function (jobType) {
    var _div_name = "divIndependentVariable";
    return {
            "job_type" : jobType,
            "data_type": window[_div_name + 'markerType'],
            "assayConstraints": {
                "patient_set": [GLOBAL.CurrentSubsetIDs[1], GLOBAL.CurrentSubsetIDs[2]],
                "ontology_term": readConceptVariables("divIndependentVariable"),
                "trial_name": null
            },
            "dataConstraints": {
                "search_keywords_ids": [window[_div_name + 'pathway']],
                "disjunctions": null
            },
            projections: ["default_real_projection"]
        };

}