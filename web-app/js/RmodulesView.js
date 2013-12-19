
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

RmodulesView.prototype.get_parameters_for_mrna = function (constraints) {

    // TODO : to be filled in with values expected by analysis job in the backend
    constraints['dataConstraints']['gene_signatures'] = null;
    constraints['dataConstraints']['genes'] = null;
    constraints['dataConstraints']['disjunction'] = null;
    constraints['dataConstraints']['pathways'] = null;
    constraints['dataConstraints']['proteins'] = null;
    constraints['dataConstraints']['homologenes'] = null;

    return constraints;
}

RmodulesView.prototype.get_parameters_for_mirna = function (constraints) {

    // TODO : to be filled in with values expected by analysis job in the backend
    constraints['dataConstraints']['disjunction'] = null;
    constraints['dataConstraints']['mirna'] = null;

    return constraints;
}

RmodulesView.prototype.get_parameters_for_rbm = function (constraints) {

    // TODO : to be filled in with values expected by analysis job in the backend
    constraints['dataConstraints']['genes'] = null;
    constraints['dataConstraints']['disjunction'] = null;
    constraints['dataConstraints']['proteins'] = null;

    return constraints;
}

RmodulesView.prototype.get_analysis_constraints = function (jobType) {

    var _div_name = "divIndependentVariable";
    var _data_type = GLOBAL.HighDimDataType;
    var _returnVal;

    // construct constraints object
    var _get_constraints_obj = function () {
        return  {
            "job_type" : jobType,
            "data_type": _data_type,
            "assayConstraints": {
                "patient_set": [GLOBAL.CurrentSubsetIDs[1], GLOBAL.CurrentSubsetIDs[2]],
                "assay_id_list": null,
                "ontology_term": readConceptVariables("divIndependentVariable"),
                "trial_name": null
            },
            "dataConstraints": {
                "disjunction": null
            },
            projections: ["default_real_projection","zscore"]
        }
    }
    _returnVal =  _get_constraints_obj();

    // do not create search_keyword_ids param if  pathway / gene is not selected
    // when gene / pathway is not selected, analysis will take up all pathways/genes.
    if (GLOBAL.CurrentPathway) {
        _returnVal['dataConstraints']['search_keyword_ids'] = [GLOBAL.CurrentPathway];
    }

    var cases =  {
        'Gene Expression':this.get_parameters_for_mrna,
        'MIRNA_QPCR':this.get_parameters_for_mirna,
        'MIRNA_SEQ':this.get_parameters_for_mirna,
        'RBM':this.get_parameters_for_rbm,
        'PROTEOMICS':this.get_parameters_for_rbm,
        'RNASEQ':this.get_parameters_for_mrna
    }

    if (cases[_data_type]) {
       _returnVal =  cases[_data_type](_returnVal);
    }

    return _returnVal;

}
