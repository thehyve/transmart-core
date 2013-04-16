/**
 * Created with IntelliJ IDEA.
 * User: riza
 * Date: 09-04-13
 * Time: 12:38
 * To change this template use File | Settings | File Templates.
 */

// Array data for the grids

Ext.grid.dummyData = [
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
 *
 * This function is called when user selects Survival Analysis aCGH from the Analysis combo box in the Dataset
 * Explorer toolbar
 *
 */
function loadSurvivalAnalysisaCGHView() {
	displayInputPanel();
}

function displayInputPanel() {

	// ----------------
	// alteration types
	// ----------------
	var alterationTypes = new Ext.form.CheckboxGroup({
		fieldLabel: 'Single Column',
		// Put all controls in a single column with width 75%
		columns: 1,
		height: 75,
		style: 'margin-left: 5px;',
		items: [
			{boxLabel: 'GAIN vs NO GAIN', name: 'cb-col-1'},
			{boxLabel: 'LOSS vs NO LOSS', name: 'cb-col-2', checked: true},
			{boxLabel: 'LOSS vs NORMAL vs GAIN', name: 'cb-col-3'}
		]
	});

	// tool bar for survival analysis acgh input
	var inputToolBar = new Ext.Toolbar({
		height: 30,
		items: ['->',{  // '->' making it right aligned
			xtype: 'button',
			text: 'Run Analysis',
			scale: 'medium',
			iconCls: 'runbutton',
			handler: function () {
				// at this moment to load grid when it's no existing yet
				// TBD: should allow user when input is changed?
				if (typeof Ext.getCmp('intermediateGridPanel') == 'undefined' ) {
					displayGrid();
				}
			}
		}]
	})


	// ------------------
	// define input panel
	// ------------------

	var p = new Ext.Panel({
		layout:'column',
		renderTo: 'analysisContainer',
		bbar: inputToolBar, // bbar
		items: [{
			xtype: 'panel',
			title: 'Step 1 - aCGH*',
			id: 'saaCGH',
			tools: [{
				id: 'refresh',
				handler: function(e, toolEl, panel, tc){
					clearInput(panel.getId());
				}
			}],
			autoScroll: true,
			columnWidth: .25,
			height: 120,
			layout: 'fit'
		},{
			title: 'Step 2 - Survival Time*',
			id: 'saSurvivalTime',
			tools: [{
				id: 'refresh',
				handler: function(e, toolEl, panel, tc){
					clearInput(panel.getId());
				}
			}],
			autoScroll: true,
			columnWidth: .25,
			height: 120,
			layout: 'fit'
		},{
			title: 'Step 3 - Censoring Variable',
			id: 'saCensoring',
			tools: [{
				id: 'refresh',
				handler: function(e, toolEl, panel, tc){
					clearInput(panel.getId());
				}
			}],
			autoScroll: true,
			columnWidth: .25,
			height: 120,
			layout: 'fit'
		},{
			title: 'Step 4 - Alteration Type*',
			id: 'saAlteration',
			columnWidth: .25,
			height: 120,
			layout: 'fit',
			items: alterationTypes
		}]
	});

	// ------------------------------
	// make panel columns as dropable
	// ------------------------------

	var dropTarget1 = Ext.get('saaCGH').select('.x-panel-bwrap .x-panel-body'); //return array

	// Add the drop targets and handler function.
	var ddTarget = new Ext.dd.DropTarget(dropTarget1.elements[0], {
			ddGroup : 'makeQuery',
			notifyDrop : dropNumericOntoCategorySelection
		}
	);

	var dropTarget2 = Ext.get('saSurvivalTime').select('.x-panel-bwrap .x-panel-body'); //return array

	// Add the drop targets and handler function.
	var ddTarget = new Ext.dd.DropTarget(dropTarget2.elements[0], {
			ddGroup : 'makeQuery',
			notifyDrop : dropNumericOntoCategorySelection
		}
	);

	var dropTarget3 = Ext.get('saCensoring').select('.x-panel-bwrap .x-panel-body'); //return array

	// Add the drop targets and handler function.
	var ddTarget = new Ext.dd.DropTarget(dropTarget3.elements[0], {
			ddGroup : 'makeQuery',
			notifyDrop : dropOntoCategorySelection
		}
	);


	// ------------------
	// define tooltips
	// ------------------

	var saaCGHTip = new Ext.ToolTip({
		target: 'saaCGH',
		html: 'Click and drag aCGH data from your selected study into this field. aCGH data is mandatory to execute ' +
			'Survival Analysis for aCGH.'
	});

	var saSurvivalTimeTip = new Ext.ToolTip({
		target: 'saSurvivalTime',
		html: 'Select time variable from the Data Set Explorer Tree and drag it into the box. For example, ' +
			'"Survival Time". This variable is required.'
	});

	var saCensoringTip = new Ext.ToolTip({
		target: 'saCensoring',
		html: 'Select the appropriate censoring variable and drag it into the box. For example, "Survival (Censor) -> ' +
			'Yes". This variable is optional.'
	});

	var saAlterationTip = new Ext.ToolTip({
		target: 'saAlteration',
		html: 'Select type alteration to perform survival analysis with.'
	});

}

function displayGrid() {

	// ------------------
	// define Grid
	// ------------------

	// tool bar for survival analysis acgh intermediate result
	var iRestToolBar = new Ext.Toolbar({
		height: 30,
		items: ['->', {
			xtype: 'button',
			text: 'Show Survival Plot',
			scale: 'medium',
			iconCls: 'chartcurvebutton'
		}]
	})

	var xg = Ext.grid;

	// shared reader
	var reader = new Ext.data.ArrayReader({}, [
		{name: 'region'},
		{name: 'cytoband'},
		{name: 'p-value', type: 'float'},
		{name: 'fdr', type: 'float'},
		{name: 'alteration'}
	]);

	// grid panel
	// ==========
	var grid = new xg.GridPanel({

		id: 'intermediateGridPanel',

		store: new Ext.data.GroupingStore({
			reader: reader,
			data: xg.dummyData,
			sortInfo:{field: 'region', direction: "ASC"},
			groupField:'alteration'
		}),

		columns: [
			{id:'region',header: "Region", width: 60, sortable: true, dataIndex: 'region'},
			{header: "Cytoband", width: 20, sortable: true, dataIndex: 'cytoband'},
			{header: "p-value", width: 20, sortable: true, dataIndex: 'p-value'},
			{header: "fdr", width: 20, sortable: true, dataIndex: 'fdr'},
			{header: "Alteration", width: 20, sortable: true, dataIndex: 'alteration'}
		],

		view: new Ext.grid.GroupingView({
			forceFit:true,
			groupTextTpl: '{text} ({[values.rs.length]} {[values.rs.length > 1 ? "Items" : "Item"]})'
		}),
		bbar: iRestToolBar,
		frame:true,
		height: 250,
		collapsible: true,
		animCollapse: false,
		title: 'Intermediate Result',
		iconCls: 'gridbutton',
		renderTo: 'analysisOutput'
	});

}

function clearInput(panelId)
{
	// get panel's body div array
	var divEl = Ext.get(panelId).select('.x-panel-bwrap .x-panel-body'); // returns array
	// get panel's body div
	var divName = divEl.elements[0];

	// Clear the drag and drop div.
	var qc = Ext.get(divName);

	for ( var i = qc.dom.childNodes.length - 1; i >= 0; i--)
	{
		var child = qc.dom.childNodes[i];
		qc.dom.removeChild(child);
	}
	clearHighDimDataSelections(divName);
	clearSummaryDisplay(divName);
}



