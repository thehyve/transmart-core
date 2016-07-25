/**
 * GLOBAL PARAMETERS FOR GENERIC COMPONENTS
 * @type {number}
 */

var dgeRnaSeqView;

/**
 * Buttons for Input Panel
 * @type {Array}
 */
var rgtInputBarBtnList = ['->', {  // '->' making it right aligned
    xtype: 'button',
    text: 'Run Analysis',
    scale: 'medium',
    iconCls: 'runbutton',
    handler: function () {
        dgeRnaSeqView.submitDgeJob();
    }
}];

var DgeRNASeqInputWidget = Ext.extend(GenericAnalysisInputBar, {

    rnaseqPanel: null,
    groupPanel: null,
    contrastPanel: null,

    statRadios: [
        {boxLabel: 'two group unpaired', name: 'stat-test-opt', XValue: 'two_group_unpaired'},
        {boxLabel: 'multi-group', name: 'stat-test-opt', XValue: 'multi_group'}
    ],

    constructor: function (config) {
        DgeRNASeqInputWidget.superclass.constructor.apply(this, arguments);
        this.init();
    },

    init: function () {

        // define child panel configs
        var childPanelConfig = [
            {
                title: 'RNASeq',
                id: 'rgt-input-rnaseq',
                isDroppable: true,
                notifyFunc: dropOntoCategorySelection,
                toolTipTitle: 'Tip: RNASeq',
                toolTipTxt: 'Drag and drop RNASeq data here.',
                columnWidth: .33
            },
            {
                title: 'Group',
                id: 'rgt-input-group',
                isDroppable: true,
                notifyFunc: dropOntoCategorySelection,
                toolTipTitle: 'Tip: Group',
                toolTipTxt: 'Drag and drop clinical variables to define multiple groups. ' +
                    'Please keep in mind that only one variable can be compared, ' +
                    'e.g. gender (female) with gender (male); ' +
                    'not gender (female) with age (>60)!',
                columnWidth: .33
            },
            {
                title: 'Contrast',
                id: 'rgt-input-contrast',
                toolTipTitle: 'Tip: Contrast',
                toolTipTxt: 'The contrast represents the biological question. There can be many questions asked, e.g.:' +
                    '<ul><li>Tumor-Normal</li> ' +
                    '<ul><li>African-European</li> ' +
                    '<li>0.5*(Control+Placebo) / Treated</li></ul>',
                columnWidth: .34
            }
        ];

        // create child panels
        this.rnaseqPanel = this.createChildPanel(childPanelConfig[0]);
        this.groupPanel = this.createChildPanel(childPanelConfig[1]);
        this.contrastPanel = this.createChildPanel(childPanelConfig[2]);

        // create contrast input field
        var contrastField = new Ext.form.Field({
            fieldLabel: '', id:'contrast', name:'contrast', labelSeparator: ' ', boxLabel:''
        });
        this.contrastPanel.add(contrastField);

        // re-draw
        this.doLayout();
    }
});

/**
 * list of result grid columns
 * @type {Array}
 * @private
 */
var _dgernaseq_grid_columns = [
    {
        id: 'genes', // id assigned so we can apply custom css (e.g. .x-grid-col-topic b { color:#333 })
        header: "genes",
        dataIndex: 'genes',
        width: 400,
        sortable: true
    },
    { id: 'logFC',
        header: "logFC",
        dataIndex: 'logFC',
        width: 100,
        sortable: true
    },
    { id: 'logCPM',
        header: "logCPM",
        dataIndex: 'logCPM',
        width: 100,
        sortable: true
    },
    { id: 'PValue',
        header: "PValue",
        dataIndex: 'PValue',
        width: 100,
        sortable: true
    },
    {
        id: 'FDR',
        header: "FDR",
        dataIndex: 'FDR',
        align: 'right',
        width: 100,
        sortable: true
    }
];

/**
 * This object represents the intermediate result grid
 * @type {*|Object}
 */
var DgeRNASeqResultGrid = Ext.extend(GenericAnalysisResultGrid, {

    jobName: '',

    constructor: function (config) {
        DgeRNASeqResultGrid.superclass.constructor.apply(this, arguments);
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
            this.plotCurvePanel = new GenericTabPlotPanel({
                id: 'plotResultCurve',
                renderTo: 'plotResultWrapper'
            });
        }
    }
});

var DgeRNASeqResultGrid = Ext.extend(GenericAnalysisResultGrid, {

    jobName: '',

    constructor: function (config) {
        DgeRNASeqResultGrid.superclass.constructor.apply(this, arguments);
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

    downloadIntermediateResult: function (jobName) {

        // clean up
        try {
            Ext.destroy(Ext.get('downloadIframe'));
        }
        catch (e) {
        }

        // get the file
        Ext.DomHelper.append(document.body, {
            tag: 'iframe',
            id: 'downloadIframe',
            frameBorder: 0,
            width: 0,
            height: 0,
            css: 'display:none;visibility:hidden;height:0px;',
            src: pageInfo.basePath + "/analysisFiles/" + jobName + "/zippedData.zip"
        });
    }
});

/**
 * This class represents the whole Group Test view
 * @type {*|Object}
 */
var DgeRNASeqView = Ext.extend(GenericAnalysisView, {

    // input panel
    inputBar: null,

    // result panel
    resultPanel: null,

    // job type
    jobType: 'DgeRNASeq',

    // job info
    jobInfo: null,

    // constructor
    constructor: function () {
        this.init();
    },

    init: function () {

        // first of all, let's reset all major components
        this.resetAll();

        // draw input panel
        this.inputBar = new DgeRNASeqInputWidget({
            id: 'rgtInputPanel',
            title: 'Input Parameters',
            iconCls: 'newbutton',
            renderTo: 'rgtContainer',
            bbar: this.createInputToolBar()
        });
    },

    redraw: function () {
        this.inputBar.doLayout();
    },

    resetAll: function () {
        this.tabIndex = 0;
        Ext.destroy(this.inputBar);
        Ext.destroy(this.resultPanel);
    },

    createInputToolBar: function () {
        var _this = this;
        return new Ext.Toolbar({
            height: 30,
            items: rgtInputBarBtnList
        });
    },

    areAllMandatoryFieldsFilled: function () {
        var isValid = true;
        var invalidInputs = [];

        var statisticalVal;

        // check if region panel is empty
        if (this.inputBar.rnaseqPanel.isEmpty()) {
            invalidInputs.push(this.inputBar.rnaseqPanel.title);
            isValid = false;
        }

        // check if group panel is empty
        if (this.inputBar.groupPanel.isEmpty()) {
            invalidInputs.push(this.inputBar.groupPanel.title);
            isValid = false;
        }

        //check if stat test values has been selected
        var contrastEl = this.inputBar.contrastPanel.getComponent('contrast');
        if (contrastEl.getValue().trim() == '') {
            isValid = false;
            invalidInputs.push(this.inputBar.contrastPanel.title);
        }

        if (!isValid) {
            var strErrMsg = 'Following needs to be defined: ';
            invalidInputs.forEach(function (item) {
                strErrMsg += '[' + item + '] ';
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

    isGroupFieldValid: function () {
        if (this.inputBar.groupPanel.getNumberOfConceptCodes() < 2) {
            Ext.MessageBox.show({
                title: 'Incorrect number of groups',
                msg: '[Group] input field should contain than one variables. Please add more variable.',
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.ERROR
            });
            return false;
        }
        return true;
    },

    validateInputs: function () {
        return this.areAllMandatoryFieldsFilled() && this.isGroupFieldValid()
    },

    createResultPlotPanel: function (jobName, view) {

        var _this = view;

        // initialize image path
        var imagePath = '';


        // get image path
        Ext.Ajax.request({
            url: pageInfo.basePath + "/DgeRNASeq/imagePath",
            method: 'POST',
            success: function (result, request) {

                imagePath = result.responseText;

                _this.resultPanel = new GenericPlotPanel({
                    id: 'plotResultCurve',
                    renderTo: 'rgtPlotWrapper',
                    width: '100%',
                    frame: true,
                    height: 600,
                    defaults: {autoScroll: true}
                });

                // Getting the template as blue print for survival curve plot.
                // Template is defined in GroupTestRNASeq.gsp

                var dgeRNASeqPlotTpl = Ext.Template.from('template-dge-rnaseq-plot');

                var groupVariable = dgeRnaSeqView.jobInfo.jobInputsJson.groupVariable;
                var groupVariableHtml = groupVariable ? groupVariable.replace('|', '<br />') : '';
                // create data instance
                var region = {
                    jobName: dgeRnaSeqView.jobInfo.name,
                    startDate: dgeRnaSeqView.jobInfo.startDate,
                    runTime: dgeRnaSeqView.jobInfo.runTime,
                    inputRNASeqVariable: dgeRnaSeqView.jobInfo.jobInputsJson.RNASeqVariable,
                    inputGroupVariable: groupVariableHtml,
                    inputAnalysisType: dgeRnaSeqView.jobInfo.jobInputsJson.contrast,
                    inputCohort1: dgeRnaSeqView.jobInfo.jobInputsJson.result_instance_id1,
                    inputCohort2: dgeRnaSeqView.jobInfo.jobInputsJson.result_instance_id2
                };

                if (imagePath) {
                    region.filename = imagePath
                }
                // generate template
                dgeRNASeqPlotTpl.overwrite(Ext.get('rgtPlotWrapper'), region);

                jQuery.get(pageInfo.basePath + '/dataExport/isCurrentUserAllowedToExport',
                    {
                        result_instance_id1: dgeRnaSeqView.jobInfo.jobInputsJson.result_instance_id1,
                        result_instance_id2: dgeRnaSeqView.jobInfo.jobInputsJson.result_instance_id2
                    },
                    function (data) {
                        if (data.result) {
                            new Ext.Button({
                                text: 'Download Result',
                                iconCls: 'downloadbutton',
                                renderTo: 'downloadBtn',
                                handler: function () {
                                    _this.downloadDgeRNASeqResult(jobName);
                                }
                            });
                        }
                    });
            },
            params: {
                jobName: jobName
            }
        });
    },


    /**
     * generates intermediate result in grid panel
     * @param data
     */
    renderResults: function (jobName, view) {

        var _this = this;

        Ext.Ajax.request({
            // retrieve information about the job (status, inputs, run-time, etc)
            url: pageInfo.basePath + "/asyncJob/getjobbyname",
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

                    baseParams: {jobName: jobName},

                    fields: [
                        'genes',
                        {name: 'logFC', type: 'float'},
                        {name: 'logCPM', type: 'float'},
                        {name: 'PValue', type: 'float'},
                        {name: 'FDR', type: 'float'}
                    ],

                    // load using script tags for cross domain, if the data in on the same domain as
                    // this page, an HttpProxy would be better
                    proxy: new Ext.data.HttpProxy({
                        url: "../DgeRNASeq/resultTable"
                    })

                });
                store.setDefaultSort('FDR', 'asc');


                // create paging bar with related store
                var pagingbar = new Ext.PagingToolbar({
                    pageSize: GEN_RESULT_GRID_LIMIT,
                    store: store,
                    displayInfo: true,
                    displayMsg: 'Displaying topics {0} - {1} of {2}',
                    emptyMsg: "No topics to display"
                });

                // make sure no instance from previous job
                Ext.destroy(Ext.get('intermediateGridPanel'));

                // create new grid and render it
                view.intermediateResultGrid = new DgeRNASeqResultGrid({
                    id: 'intermediateGridPanel',
                    title: 'Intermediate Result - Job Name: ' + jobName,
                    renderTo: 'intermediateResultWrapper',
                    trackMouseOver: false,
                    loadMask: true,
                    columns: _dgernaseq_grid_columns,
                    store: store,
                    bbar: pagingbar,
                    jobName: jobName
                });

                view.intermediateResultGrid.render();

                // finally load the data
                store.load({params: {start: 0, limit: GEN_RESULT_GRID_LIMIT}});

                _this.createResultPlotPanel(jobName, view);
            },
            failure: function (result, request) {
                console.log('failure ....')
            },
            params: {
                jobName: jobName
            }
        })

    },

    downloadDgeRNASeqResult: function (jobName) {

        // clean up
        try {
            Ext.destroy(Ext.get('downloadIframe'));
        }
        catch (e) {
        }

        // get the file
        Ext.DomHelper.append(document.body, {
            tag: 'iframe',
            id: 'downloadIframe',
            frameBorder: 0,
            width: 0,
            height: 0,
            css: 'display:none;visibility:hidden;height:0px;',
            src: pageInfo.basePath + "/analysisFiles/" + jobName + "/zippedData.zip"
        });
    },

    onJobFinish: function (jobName, view) {
        this.renderResults(jobName, view);
    },

    submitDgeJob: function () {
        var _this = this;

        // Fill global subset ids if null
        if ((!isSubsetEmpty(1) && GLOBAL.CurrentSubsetIDs[1] == null) ||
            (!isSubsetEmpty(2) && GLOBAL.CurrentSubsetIDs[2] == null)) {
            runAllQueries(function() {_this.submitDgeJob();});
            return;
        }

        if (this.validateInputs()) {

            var rnaseqVal = this.inputBar.rnaseqPanel.getConceptCode();
            var groupVals = this.inputBar.groupPanel.getConceptCodes();
            var contrastComponent = this.inputBar.contrastPanel.getComponent('contrast');
            var contrastVal = contrastComponent.getValue();

            // create a string of all the concepts we need for the i2b2 data.
            var variablesConceptCode = rnaseqVal;
            variablesConceptCode += groupVals != '' ? "|" + groupVals : "";

            // compose params
            var formParams = {
                RNASeqVariable: rnaseqVal,
                groupVariable: groupVals,
                contrast: contrastVal,
                variablesConceptPaths: variablesConceptCode,
                analysisConstraints: JSON.stringify({
                    "job_type": _this.jobType,
                    "data_type": "rnaseq",
                    "assayConstraints": {
                        "patient_set": [GLOBAL.CurrentSubsetIDs[1], GLOBAL.CurrentSubsetIDs[2]],
                        "assay_id_list": null,
                        "ontology_term": [
                            {
                                'term': rnaseqVal,
                                'options': {'type': "default"}
                            }
                        ],
                        "trial_name": null
                    },
                    "dataConstraints": {
                        "disjunction": null
                    },
                    "projections": ["rnaseq_values"]
                }),
                jobType: _this.jobType
            };

            var job = this.submitJob(formParams, this.onJobFinish, this);
        }

    }

});

/**
 * Invoked when user selects the analysis from combo box
 */
function loadDgeRNASeqView() {
    dgeRnaSeqView = new DgeRNASeqView();
}
