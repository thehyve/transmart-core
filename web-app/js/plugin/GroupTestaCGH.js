/**
 * User: riza
 * Date: 23-04-13
 * Time: 11:28
 */


var GroupTestInputBar = Ext.extend(InputBar, {

	regionPanel: null,
	groupPanel: null,
	statTestPanel: null,
	alterationPanel: null,

	statCheckBoxes: [
		{boxLabel: 'Chi-square', name: 'st-col-1'},
		{boxLabel: 'Wilcoxon', name: 'st-col-2'},
		{boxLabel: 'Kruskal-Wallis', name: 'st-col-3'}
	],

	alterationCheckboxes: [
		{boxLabel: 'GAIN vs NO GAIN', name: 'cb-col-1'},
		{boxLabel: 'LOSS vs NO LOSS', name: 'cb-col-2'},
		{boxLabel: 'LOSS vs NORMAL vs GAIN', name: 'cb-col-3'}
	],

	constructor: function(config) {
		GroupTestInputBar.superclass.constructor.apply(this, arguments);
		this.init();
	},

	init: function() {

		// define child panel configs
		var childPanelConfig = [{
			title: 'Region',
			id:  'gt-input-region',
			isDroppable: true,
			notifyFunc: dropNumericOntoCategorySelection,
			toolTipTxt: 'Tool-tip for Region'

		},{
			title: 'Group',
			id:  'gt-input-group',
			isDroppable: true,
			notifyFunc: dropOntoCategorySelection,
			toolTipTxt: 'Tool-tip for Group'

		},{
			title: 'Statistical Test',
			id: 'gt-input-stat-test',
			toolTipTxt: 'Tool-tip for Statistical Test'
		},{
			title: 'Alteration Type',
			id:  'gt-input-alteration',
			toolTipTxt: 'Tool-tip for Alteration Type'
		}];

		// create child panels
		this.regionPanel = this.createChildPanel(childPanelConfig[0]);
		this.groupPanel = this.createChildPanel(childPanelConfig[1]);
		this.statTestPanel = this.createChildPanel(childPanelConfig[2]);
		this.alterationPanel = this.createChildPanel(childPanelConfig[3]);

		// create check boxes
		this.statTestPanel.add(this.createCheckBoxForm(this.statCheckBoxes));
		this.alterationPanel.add(this.createCheckBoxForm(this.alterationCheckboxes));

		// re-draw
		this.doLayout();
	}
});


/**
 * This class represents the whole Group Test view
 * @type {*|Object}
 */
var GroupTestView = Ext.extend(Object, {

	// input panel
	inputPanel : null,

	// result panel
	resultPanel : null,

	// constructor
	constructor: function() {
		this.init();
	},

	init: function() {
		// draw input panel
		GroupTestView.inputPanel = new GroupTestInputBar({
			id: 'gtInputPanel',
			title: 'Input Parameters',
			iconCls: 'newbutton',
			renderTo: 'gtContainer',
			bbar: this.createInputToolBar()
		});
	},

	createInputToolBar: function() {
		return new Ext.Toolbar({
			height: 30,
			items: ['->',{  // '->' making it right aligned
				xtype: 'button',
				text: 'Run Analysis',
				scale: 'medium',
				iconCls: 'runbutton',
				handler: function () {
					// TODO: add run analysis handler
					console.log('LOG: inside run analysis handler()');
				}
			}]
		});
	}

});

/**
 * Invoked when user selects Group Test aCGH from Analysis combo box
 */
function loadGroupTestaCGHView() {

	console.log("LOG: eval loadGroupTestaCGHView ...") ;

	// everything starts here ..
	var groupTestView = new GroupTestView();
}