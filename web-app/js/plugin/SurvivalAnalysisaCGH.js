/**
 * Created with IntelliJ IDEA.
 * User: riza
 * Date: 26-04-13
 * Time: 09:18
 * To change this template use File | Settings | File Templates.
 */


/**
 * dummy data for intermediate result
 */
var dummyData = [
	['Chr1:7282393-152722828',"1p36.33-p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:31432123-23232322',"1p36.33-p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:1232323-123344552',"p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:2323123-232312222',"1p36.31",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:55123123-62342342',"1p36.33-p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:7234324-234324343',"1p36.33-p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:23423443-26534343',"p36.13",0.02,0.03, 'GAIN vs NO GAIN'],
	['Chr1:62342342-23423444',"p36.13",0.02,0.03, 'GAIN vs NO GAIN'],

	['Chr1:62342342-23423444',"p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:514212-1232323222',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:51231233-22212332',"p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:8234342-343434333',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:623423433-1232323',"p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:24232321-12323232',"p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:12323233-71231232',"p36.13",0.02,0.03, 'LOSS vs NO LOSS'],
	['Chr1:22232323-12333223',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NO LOSS'],

	['Chr1:62342342-23423444',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:24232321-23423444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:1232323-234232444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:62342342-12323w23',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:55123123-23423444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:24232321-23423444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:55123123-23423444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:55123123-24232321',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:62342342-55123123',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:24232321-23423444',"1p36.33-p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN'],
	['Chr1:62342342-23423444',"p36.13",0.02,0.03, 'LOSS vs NORMAL vs GAIN']
];


/**
 * View instance
 */
var survivalAnalysisACGHView;

/**
 * Buttons for Input Panel
 * @type {Array}
 */
var saInputPanelBtnList = ['->',{  // '->' making it right aligned
	xtype: 'button',
	text: 'Run Analysis',
	scale: 'medium',
	iconCls: 'runbutton',
	handler: function () {
		survivalAnalysisACGHView.submitSurvivalAnalysisaCGHJob();
	}
}];

/**
 * Buttons for Intermediate Result Panel
 * @type {Array}
 */
var saIntermediatePanelBtnList = ['->',
	{
		xtype: 'button',
		text: 'Download Result',
		scale: 'medium',
		iconCls: 'downloadbutton',
		handler: function (b, e) {
			// ****************************************************************** //
			// TODO: To provide handler for download Intermediate result data     //
			// ****************************************************************** //
		}
	},
	{
		xtype: 'button',
		text: 'Show Survival Plot',
		scale: 'medium',
		iconCls: 'chartcurvebutton',
		handler: function (b, e) {

			// get selected regions
			var selectedRows = survivalAnalysisACGHView.getSelectionsPanel();
			if (selectedRows.length > 0) {
				// invoke display survival plot
				survivalAnalysisACGHView.displaySurvivalPlot(selectedRows);
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
 * This object represents Input Bar in the Survival Analysis page
 * @type {*|Object}
 */
var SurvivalAnalysisACGHInputWidget = Ext.extend(InputBar, {

	regionPanel: null,
	survivalPanel: null,
	censoringPanel: null,
	alterationPanel: null,

	alterationCheckboxes: [
		{boxLabel: 'GAIN vs NO GAIN', name: 'cb-col-1'},
		{boxLabel: 'LOSS vs NO LOSS', name: 'cb-col-2'},
		{boxLabel: 'LOSS vs NORMAL vs GAIN', name: 'cb-col-3'}
	],

	constructor: function(config) {
		SurvivalAnalysisACGHInputWidget.superclass.constructor.apply(this, arguments);
		this.init();
	},

	init: function() {

		// define child panel configs
		var childPanelConfig = [{
			title: 'Region',
			id:  'sa-input-region',
			isDroppable: true,
			notifyFunc: dropNumericOntoCategorySelection,
			toolTipTxt: 'Tool-tip for Region'

		},{
			title: 'Survival Time',
			id:  'sa-input-survival',
			isDroppable: true,
			notifyFunc: dropOntoCategorySelection,
			toolTipTxt: 'Tool-tip for Survival Time'

		},{
			title: 'Censoring Variable',
			id: 'gt-input-censoring',
			isDroppable: true,
			notifyFunc: dropOntoCategorySelection,
			toolTipTxt: 'Tool-tip for Censoring variable'
		},{
			title: 'Alteration Type',
			id:  'gt-input-alteration',
			toolTipTxt: 'Tool-tip for Alteration Type'
		}];

		// create child panels
		this.regionPanel = this.createChildPanel(childPanelConfig[0]);
		this.survivalPanel = this.createChildPanel(childPanelConfig[1]);
		this.censoringPanel = this.createChildPanel(childPanelConfig[2]);
		this.alterationPanel = this.createChildPanel(childPanelConfig[3]);

		// create check boxes
		this.alterationPanel.add(this.createCheckBoxForm(this.alterationCheckboxes));

		// re-draw
		this.doLayout();
	}

});

/**
 * This object represent the whole Survival Analysis Array CGH View
 * @type {*|Object}
 */
var SurvivalAnalysisACGHView = Ext.extend(Object, {

	// input panel
	inputPanel : null,

	// intermediate result panel
	intermediateResultPanel : null,

	// plot curve panel
	plotCurvePanel : null,

	//tab index
	tabIndex: 0,

	constructor: function () {
		this.init();
	},

	init: function() {
		// draw input panel
		SurvivalAnalysisACGHView.inputPanel = new SurvivalAnalysisACGHInputWidget({
			title: 'Input Parameters',
			iconCls: 'newbutton',
			renderTo: 'analysisContainer',
			bbar: this.createInputToolBar(saInputPanelBtnList)
		});
	},

	createInputToolBar: function(btnList) {
		return new GenericToolBar({
			items: btnList
		});
	},

	createIntermediateResultToolBar: function(btnList) {
		return new GenericToolBar({
			items:  btnList
		});
	},

	submitSurvivalAnalysisaCGHJob: function() {

		/*
		 TODO: keep this code to do validation later
		 var aCGHVal = Ext.get('saaCGH').select('.x-panel-bwrap .x-panel-body').item(0);
		 var survivalTimeVal = Ext.get('saSurvivalTime').select('.x-panel-bwrap .x-panel-body').item(0);
		 var censoringVal = Ext.get('saCensoring').select('.x-panel-bwrap .x-panel-body').item(0);
		 */

		/**
		 * TODO: get check boxes values
		 */

		//if ( aCGHVal.dom.childNodes.length > 0 && survivalTimeVal.dom.childNodes.length > 0) {

		/**
		 * TODO: submit job to backend and get the result
		 */

		this.generateResultGrid(dummyData);

		/*
		TODO: keep this code to do validation later
		} else {
			console.error('[TRANSMART ERROR] Cannot Run Analysis: Missing some mandatory arguments');
			return false;
		}
		*/

	},

	generateResultGrid: function (data) {

		Ext.grid.intermediateResultData = data;

		var gridReader = new Ext.data.ArrayReader({}, [
			{name: 'region'},
			{name: 'cytoband'},
			{name: 'pvalue', type: 'float'},
			{name: 'fdr', type: 'float'},
			{name: 'alteration'}
		]);

		var groupStore =  new Ext.data.GroupingStore({
			reader: gridReader,
			data: Ext.grid.intermediateResultData,
			sortInfo:{field: 'region', direction: "ASC"},
			groupField:'alteration'
		})

		SurvivalAnalysisACGHView.intermediateResultPanel = new ResultGridPanel({
			store: groupStore,
			bbar: this.createIntermediateResultToolBar(saIntermediatePanelBtnList)
		});

		SurvivalAnalysisACGHView.intermediateResultPanel.doLayout();

	},

	getSelectionsPanel: function () {
		return SurvivalAnalysisACGHView.intermediateResultPanel.getSelectionModel().getSelections();
	},

	displaySurvivalPlot: function (selectedRegions) {
		// create tab panel as container if it's not existing
		if (typeof Ext.getCmp('plotResultCurve') == 'undefined' ) {

			new Ext.TabPanel({
				id: 'plotResultCurve',
				renderTo: 'plotResultWrapper',
				width:'100%',
				frame:true,
				height:600,
				defaults: {autoScroll:true}
			});

		}

		// Getting the template as blue print for survival curve plot.
		// Template is defined in SurvivalAnalysisaCGH.gsp
		var survivalPlotTpl = Ext.Template.from('template-survival-plot');

		//get tab panel
		var resultTabPanel = Ext.getCmp('plotResultCurve');

		// generate plot for every selected regions
		for (var i = 0; i < selectedRegions.length ; i++) {

			// ****************************************************************** //
			// TODO: Somewhere here invoke R script to get Survival Analysis Plot //
			// ****************************************************************** //

			var strToolBarId = 'plotCurveToolBarId_' + this.tabIndex + "_" + i;

			// create data instance
			var region = {
				region:  selectedRegions[i].data.region,
				cytoband:  selectedRegions[i].data.cytoband,
				pvalue:  selectedRegions[i].data.pvalue,
				fdr:  selectedRegions[i].data.fdr,
				alteration:  selectedRegions[i].data.alteration,
				plotCurveToolBarId: strToolBarId,
				foldername: 'guest-SurvivalAnalysis-102086',
				filename: 'SurvivalCurve2.png'
			};

			// create tab item
			var p = resultTabPanel.add({
				id: 'tabPlotResult_' + ++this.tabIndex,
				title: selectedRegions[i].data.region,
				closable:true
			});

			//set active tab
			resultTabPanel.setActiveTab(p);

			//redo layout
			resultTabPanel.doLayout();

			// generate template with associated region values in selected tab
			survivalPlotTpl.overwrite(Ext.get('tabPlotResult_' + this.tabIndex), region);

			// create export button
			var exportBtn = new Ext.Button ({
				text : 'Download Survival Plot',
				iconCls : 'downloadbutton',
				renderTo: strToolBarId
			});
		}
	}

});


/**
 *
 * This function is called when user selects Survival Analysis aCGH from the Analysis combo box in the Dataset
 * Explorer toolbar
 *
 */
function loadSurvivalAnalysisaCGHView() {
	// everything starts here ..
	survivalAnalysisACGHView = new SurvivalAnalysisACGHView();
}


