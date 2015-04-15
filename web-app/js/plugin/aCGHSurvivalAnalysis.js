/**
 * GLOBAL PARAMETERS FOR GENERIC COMPONENTS
 * @type {number}
 */
var SA_JOB_TYPE = 'aCGHSurvivalAnalysis';

/**
 * survival analysis acgh view instance
 */
var survivalAnalysisACGHView;

/**
 * Buttons for Input Panel
 * @type {Array}
 */
var _inputpanel_items = ['->', // '->' making it right aligned
    'Permutations:',
    {
        xtype: 'textfield',
        name: 'permutation',
        id: 'permutation',
        width: 50,
        value: 10000
    },
    {
        xtype: 'button',
        text: 'Run Analysis',
        scale: 'medium',
        iconCls: 'runbutton',
        handler: function () {
            survivalAnalysisACGHView.submitACGHSurvivalAnalysisJob();
        }
    }];

/**
 * list of buttons for intermediate result grid
 * @type {Array}
 * @private
 */
var _resultgrid_items = [
    {
        xtype: 'button',
        text: 'Show Survival Plot',
        scale: 'medium',
        iconCls: 'chartcurvebutton',
        handler: function (b, e) {
            // get selected regions
            var selectedRows = survivalAnalysisACGHView.intermediateResultGrid.getSelectedRows();
            if (selectedRows.length > 0) {
                // invoke display survival plot
                survivalAnalysisACGHView.intermediateResultGrid.displaySurvivalPlot(selectedRows);
            } else {
                // warn user if there's no row is selected
                Ext.MessageBox.show({
                    title: 'No row is selected',
                    msg: 'To display the survival plot, you must select at least one region from the grid.',
                    buttons: Ext.MessageBox.OK,
                    icon: Ext.MessageBox.INFO
                });
            }
        }
    }];

/**
 * list of result grid columns
 * @type {Array}
 * @private
 */
var _resultgrid_columns = [{
    id: 'chromosome', // id assigned so we can apply custom css (e.g. .x-grid-col-topic b { color:#333 })
    header: "chromosome",
    dataIndex: 'chromosome',
    width: 400,
    sortable: true
},{
    header: "cytoband",
    dataIndex: 'cytoband',
    width: 100,
    sortable: true
},{
    header: "start",
    dataIndex: 'start',
    width: 100,
    sortable: true
},{
    header: "end",
    dataIndex: 'end',
    width: 100,
    sortable: true
},{
    header: "pvalue",
    dataIndex: 'pvalue',
    width: 100,
    align: 'right',
    sortable: true
},{
    id: 'fdr',
    header: "fdr",
    dataIndex: 'fdr',
    align: 'right',
    width: 100,
    sortable: true
}];

/**
 * This object represents Input Bar in the Survival Analysis page
 * @type {*|Object}
 */
var SurvivalAnalysisInputBar = Ext.extend(GenericAnalysisInputBar, {

    regionPanel: null,
    survivalPanel: null,
    censoringPanel: null,
    alterationPanel: null,

    alterationRadioButtons: [
        {boxLabel: 'GAIN vs NO GAIN', name: 'rb-alt', XValue:'1'},
        {boxLabel: 'LOSS vs NO LOSS', name: 'rb-alt', XValue:'-1'},
        {boxLabel: 'LOSS vs NORMAL vs GAIN', name: 'rb-alt', XValue:'0'}
    ],

    constructor: function(config) {
        SurvivalAnalysisInputBar.superclass.constructor.apply(this, arguments);
        this.init();
    },

    init: function() {

        // define child panel configs
        var childPanelConfig = [{
            title: 'Region',
            id:  'sa-input-region',
            isDroppable: true,
            notifyFunc: dropOntoCategorySelection,
            toolTipTitle: 'Tip: Region',
            toolTipTxt: 'Drag and drop aCGH region here.'
        },{
            title: 'Survival Time',
            id:  'sa-input-survival',
            isDroppable: true,
            notifyFunc: dropNumericOntoCategorySelection,
            toolTipTitle: 'Tip: Survival Time',
            toolTipTxt: 'Drag and drop phenodata with survival data.'
        },{
            title: 'Censoring Variable',
            id: 'sa-input-censoring',
            isDroppable: true,
            notifyFunc: dropOntoCategorySelection,
            toolTipTitle: 'Tip: Censoring Variable',
            toolTipTxt: 'Drag and drop survival status (e.g alive or dead).'
        },{
            title: 'Alteration Type',
            id:  'sa-input-alteration',
            toolTipTitle: 'Tip: Alteration Type',
            toolTipTxt: 'Select type of chromosomal alteration to test the association.'
        }];

        // create child panels
        this.regionPanel = this.createChildPanel(childPanelConfig[0]);
        this.survivalPanel = this.createChildPanel(childPanelConfig[1]);
        this.censoringPanel = this.createChildPanel(childPanelConfig[2]);
        this.alterationPanel = this.createChildPanel(childPanelConfig[3]);

        // create check boxes
        this.alterationPanel.add(this.createRadioBtnGroup(this.alterationRadioButtons, 'alteration-types-chk-group'));

        // re-draw
        this.doLayout();
    }

});


/**
 * This object represents the intermediate result grid
 * @type {*|Object}
 */
var IntermediateResultGrid = Ext.extend(GenericAnalysisResultGrid, {

    jobName : '',

    constructor: function(config) {
        IntermediateResultGrid.superclass.constructor.apply(this, arguments);
        this.init();
    },

    init: function () {

    },

    /**
     * Get selected rows from the grid
     * @returns {*}
     */
    getSelectedRows: function () {
        return this.getSelectionModel().getSelections();
    },

    /**
     * Display selected rows in tab panel
     * @param selectedRegions
     */
    displaySurvivalPlot: function (selectedRegions) {

        var outer_scope = this;

        // creating tab panel
        if (this.plotCurvePanel == null) {
            this.plotCurvePanel =  new GenericTabPlotPanel({
                id: 'plotResultCurve',
                renderTo: 'plotResultWrapper'
            });
        }

        // create tab as many as selected rows
        for (var i = 0; i < selectedRegions.length ; i++) {

            var imagePath = '';
            var data = selectedRegions[i].data;
            var _me = this;

            // get image path
            Ext.Ajax.request({
                url: pageInfo.basePath+"/SurvivalAnalysisResult/imagePath",
                method: 'POST',
                success: function(result, request){

                    imagePath = result.responseText;

                    // compose tab_id form region name + cytoband + alteration type
                    var tab_id = 'survival_' +  data.chromosome + '_' + data.start + '_' + data.end;
                    tab_id = tab_id.replace(/\s/g,'');   // remove whitespaces

                    // Getting the template as blue print for survival curve plot.
                    // Template is defined in SurvivalAnalysisaCGH.gsp
                    var survivalPlotTpl = Ext.Template.from('template-survival-plot');

                    var translatedAlteration = survivalAnalysisACGHView.translateAlteration(
                        survivalAnalysisACGHView.jobInfo.jobInputsJson.aberrationType
                    );

                    var censoringVariable = survivalAnalysisACGHView.jobInfo.jobInputsJson.censoringVariable;
                    var censoringVariableHtml = censoringVariable ? censoringVariable.replace('|', '<br />') : '';
                    // create data instance
                    var region = {
                        jobName: survivalAnalysisACGHView.jobInfo.name,
                        startDate: survivalAnalysisACGHView.jobInfo.startDate,
                        runTime: survivalAnalysisACGHView.jobInfo.runTime,
                        inputRegion: survivalAnalysisACGHView.jobInfo.jobInputsJson.regionVariable,
                        inputSurvivalTime: survivalAnalysisACGHView.jobInfo.jobInputsJson.timeVariable,
                        inputCensoring: censoringVariableHtml,
                        inputCohort1: survivalAnalysisACGHView.jobInfo.jobInputsJson.result_instance_id1,
                        inputCohort2: survivalAnalysisACGHView.jobInfo.jobInputsJson.result_instance_id2,
                        inputAlteration: translatedAlteration,
                        chromosome:  data.chromosome,
                        cytoband:  data.cytoband,
                        start:  data.start,
                        end:  data.end,
                        pvalue:  data.pvalue,
                        fdr:  data.fdr,
                        filename: imagePath
                    };

                    //create tab title
                    var tab_title = 'Chr' + region.chromosome + ' (' + region.start + '-' + region.end + ')';

                    // add tab to the container
                    _me.plotCurvePanel.addTab(region, tab_id, survivalPlotTpl, tab_title);

                },
                failure: function(result, request){
                    Ext.Msg.alert('Status', 'Unable to get image');
                },
                timeout: '1800000',
                params: {
                    jobName: this.jobName,
                    jobType: SA_JOB_TYPE,
                    chromosome:  selectedRegions[i].data.chromosome,
                    start:  selectedRegions[i].data.start,
                    end:  selectedRegions[i].data.end,
                    alteration:  selectedRegions[i].data.alteration
                }
            });

        }

    },

    downloadIntermediateResult: function (jobName) {

        // clean up
        try {
            Ext.destroy(Ext.get('downloadIframe'));
        }
        catch(e) {}

        // get the file
        Ext.DomHelper.append(document.body, {
            tag: 'iframe',
            id:'downloadIframe',
            frameBorder: 0,
            width: 0,
            height: 0,
            css: 'display:none;visibility:hidden;height:0px;',
            src: pageInfo.basePath + "/analysisFiles/" + jobName + "/zippedData.zip"
        });
    }

});

/**
 * This object represents the whole Survival Analysis Array CGH View
 * @type {*|Object}
 */
var SurvivalAnalysisACGHView = Ext.extend(GenericAnalysisView, {

    // input panel
    inputBar : null,

    // intermediate result panel
    intermediateResultGrid : null,

    // plot curve panel
    plotCurvePanel : null,

    // job info
    jobInfo : null,

    constructor: function () {
        this.init();
    },

    redraw: function () {
        this.inputBar.doLayout();
        if (this.intermediateResultGrid) {
            this.intermediateResultGrid.doLayout();
        }
    },

    init: function() {

        // first of all, let's reset all major components
        this.resetAll();

        // start drawing  input panel
        this.inputBar = new SurvivalAnalysisInputBar({
            id: 'survivalAnalysisACGHInputBar',
            title: 'Input Parameters',
            iconCls: 'newbutton',
            renderTo: 'analysisContainer',
            bbar: this.createToolBar(_inputpanel_items)
        });
    },

    resetAll: function () {
        // destroy input bar
        Ext.destroy(Ext.get('survivalAnalysisACGHInputBar'));
        this.inputBar = null;

        // and destroy the rest ..
        this.resetResult();
    },

    resetResult: function () {
        // destroy intermediate grid
        Ext.destroy(Ext.get('intermediateGridPanel'));
        this.intermediateResultGrid = null;

        // destroy plot result curve
        Ext.destroy(Ext.get('plotResultCurve'));
        this.plotCurvePanel = null;
    },

    createToolBar: function(btnList) {
        return new GenericAnalysisToolBar({
            items: btnList
        });
    },

    submitACGHSurvivalAnalysisJob: function() {
        var _this = this;

        // Fill global subset ids if null
        if ((!isSubsetEmpty(1) && GLOBAL.CurrentSubsetIDs[1] == null) ||
            (!isSubsetEmpty(2) && GLOBAL.CurrentSubsetIDs[2] == null)) {
            runAllQueries(function() {_this.submitACGHSurvivalAnalysisJob();});
            return;
        }

        if (this.validateInputs()) {
            var regionEl =  this.inputBar.regionPanel.getInputEl();
            var errorMessage = "One or more of the high dimensional data nodes are not acgh data; select a copy number regions file for this analysis";
            this.validateDataTypes(regionEl, ["acgh"], errorMessage, function() {
                console.log('LOG: submit survival analysis acgh job');

                // get concept codes
                var variablesConceptCode = '';
                var regionVarConceptCode = _this.inputBar.regionPanel.getConceptCode();
                var survivalVarConceptCode = _this.inputBar.survivalPanel.getConceptCode();
                var censoringVarConceptCode = _this.inputBar.censoringPanel.getConceptCodes();
                var permutationComponent = Ext.get('permutation');
                var permutation = permutationComponent.getValue();

                // get alteration value
                var alterationBtnGroup = _this.inputBar.alterationPanel.getComponent('alteration-types-chk-group');
                var alterationVal = alterationBtnGroup.getSelectedValue();

                // create a string of all the concepts we need for the i2b2 data.
                variablesConceptCode = regionVarConceptCode;
                variablesConceptCode += survivalVarConceptCode != '' ? "|" + survivalVarConceptCode : "";
                variablesConceptCode += censoringVarConceptCode != '' ? "|" + censoringVarConceptCode : "";

                // compose params
                var formParams = {
                    regionVariable: regionVarConceptCode,
                    timeVariable: survivalVarConceptCode,
                    censoringVariable: censoringVarConceptCode,
                    variablesConceptPaths: variablesConceptCode,
                    aberrationType: alterationVal,
                    numberOfPermutations: permutation,
                    confidenceIntervals: '',
                    jobType: SA_JOB_TYPE,
                    analysisConstraints: JSON.stringify({
                        "job_type": SA_JOB_TYPE,
                        "data_type": "acgh",
                        "assayConstraints": {
                            "patient_set": [GLOBAL.CurrentSubsetIDs[1], GLOBAL.CurrentSubsetIDs[2]],
                            "assay_id_list": null,
                            "ontology_term": [
                                {
                                    'term': regionVarConceptCode,
                                    'options': {'type': "default"}
                                }
                            ],
                            "trial_name": null
                        },
                        "dataConstraints": {
                            "disjunction": null
                        },
                        "projections": ["acgh_values"]
                    })
                };

                // reset previous analysis result
                _this.resetResult();

                // submit job
                var job = _this.submitJob(formParams, _this.renderResults, _this);
            });

        }
    },

    validateInputs: function () {

        var isValid = true;
        var invalidInputs = [];

        var regionVal;
        var survivalAnalysisVal;
        var censoringVal;
        var alterationValues;

        // check if region panel is empty
        if (this.inputBar.regionPanel.isEmpty()) {
            invalidInputs.push(this.inputBar.regionPanel.title);
            isValid = false;
        } else {
            regionVal =  this.inputBar.regionPanel.getInputEl();
        }

        // check if survival time  panel is empty
        if (this.inputBar.survivalPanel.isEmpty()) {
            invalidInputs.push(this.inputBar.survivalPanel.title);
            isValid = false;
        } else {
            survivalAnalysisVal =  this.inputBar.survivalPanel.getInputEl();
        }

        // check censoring variable is empty (e.g status is dead or alive)
        if (this.inputBar.censoringPanel.isEmpty()) {
            invalidInputs.push(this.inputBar.censoringPanel.title);
            isValid = false;
        } else {
            censoringVal =  this.inputBar.censoringPanel.getInputEl();
        }

        //check if alteration values has been selected
        var alterationChkGroup = this.inputBar.alterationPanel.getComponent('alteration-types-chk-group');
        alterationValues =  alterationChkGroup.getSelectedValue();
        if (!alterationValues) {
            isValid = false;
            invalidInputs.push(this.inputBar.alterationPanel.title);
        }

        var permutationEl = Ext.get('permutation');
        if (permutationEl.getValue().trim() == '' || isNaN(permutationEl.getValue())) {
            isValid = false;
            invalidInputs.push('Permutations');
        }

        if (!isValid) {
            var strErrMsg = 'Following needs to be defined: ';
            invalidInputs.each(function (item) {
                strErrMsg += '['+item + '] ';
            })

            // inform user on mandatory inputs need to be defined
            Ext.MessageBox.show({
                title: 'Missing mandatory inputs',
                msg: strErrMsg,
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.ERROR
            });

        }

        return isValid;
    },


    /**
     * generates intermediate result in grid panel
     * @param data
     */
    renderResults: function (jobName, view) {

        var _this = this;

        Ext.Ajax.request ({
            // retrieve information about the job (status, inputs, run-time, etc)
            url: pageInfo.basePath+"/asyncJob/getjobbyname",
            method: 'GET',
            success: function (result, request) {

                var resultJSON = JSON.parse(result.responseText);
                _this.jobInfo = resultJSON.jobs[0];

                // create store data
                var store = new Ext.data.JsonStore({
                    root: 'result',
                    totalProperty: 'totalCount',
                    idProperty: 'threadid',
                    remoteSort: true,   // can be enhanced with remote sort

                    baseParams: {jobName:jobName},

                    fields: [
                        'chromosome',
                        'cytoband',
                        {name: 'start', type: 'int'},
                        {name: 'end', type: 'int'},
                        {name: 'pvalue', type: 'float'},
                        {name: 'fdr', type: 'float'}
                    ],

                    // load using script tags for cross domain, if the data in on the same domain as
                    // this page, an HttpProxy would be better
                    proxy: new Ext.data.HttpProxy({
                        url: pageInfo.basePath + "/SurvivalAnalysisResult/list"
                    })

                });
                store.setDefaultSort('chromosome', 'asc');

                var finishRendering = function(menuButtons) {
                    // create paging bar with related store
                    var pagingbar = new Ext.PagingToolbar({
                        pageSize: GEN_RESULT_GRID_LIMIT,
                        store: store,
                        displayInfo: true,
                        displayMsg: 'Displaying topics {0} - {1} of {2}',
                        emptyMsg: "No topics to display",
                        items: menuButtons
                    });

                    // make sure no instance from previous job
                    Ext.destroy(Ext.get('intermediateGridPanel'));

                    // create new grid and render it
                    view.intermediateResultGrid  = new IntermediateResultGrid({
                        id: 'intermediateGridPanel',
                        title: 'Intermediate Result - Job Name: ' + jobName ,
                        renderTo: 'intermediateResultWrapper',
                        trackMouseOver:false,
                        loadMask: true,
                        columns: _resultgrid_columns,
                        store: store,
                        bbar: pagingbar,
                        jobName: jobName
                    });

                    view.intermediateResultGrid.render();

                    // finally load the data
                    store.load({params:{start:0, limit:GEN_RESULT_GRID_LIMIT}});
                }

                jQuery.ajax({
                        url: pageInfo.basePath + '/dataExport/isCurrentUserAllowedToExport',
                        type: 'GET',
                        data: {
                            result_instance_id1: survivalAnalysisACGHView.jobInfo.jobInputsJson.result_instance_id1,
                            result_instance_id2: survivalAnalysisACGHView.jobInfo.jobInputsJson.result_instance_id2
                        },
                        success: function(data) {
                            var menuButtons = _resultgrid_items
                            if (data.result) {
                                menuButtons = menuButtons.concat({
                                    xtype: 'button',
                                    text: 'Download Result',
                                    scale: 'medium',
                                    iconCls: 'downloadbutton',
                                    handler: function (b, e) {
                                        // get job name
                                        var jobName = survivalAnalysisACGHView.intermediateResultGrid.jobName;
                                        return survivalAnalysisACGHView.intermediateResultGrid.downloadIntermediateResult(jobName);
                                    }
                                });
                            }

                            finishRendering(menuButtons);
                        },
                        fail: function(data) {
                            finishRendering(_resultgrid_items);
                        }
                });
            },
            failure: function (result, request) {
                console.log('failure ....')
            },
            params: {
                jobName: jobName
            }
        })

    }

});

/**
 *
 * This function is called when user selects Survival Analysis aCGH from the Analysis combo box in the Dataset
 * Explorer toolbar
 *
 */
function loadACGHSurvivalAnalysisView() {
    // everything starts here ..
    survivalAnalysisACGHView = new SurvivalAnalysisACGHView();
}
