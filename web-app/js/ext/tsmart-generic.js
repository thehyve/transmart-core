/**
 * User: riza
 * Date: 25-04-13
 * Time: 15:54
 */


/**
 * GLOBAL PARAMETERS FOR GENERIC COMPONENTS
 * @type {number}
 */
var GEN_RESULT_GRID_LIMIT = 25;


GenericToolTip = Ext.extend(Ext.ToolTip, {
	html: null,
	autoHide: true,
	constructor: function(config) {
		GenericToolTip.superclass.constructor.apply(this, arguments);
	}
});


/**
 * redraw input bar when window is being resized
 */
Ext.EventManager.onWindowResize(function() {

    // redraw survival analysis view
    if (survivalAnalysisACGHView) survivalAnalysisACGHView.redraw();
    // redraw group test view
    if (groupTestView) groupTestView.redraw();

});

/**
 * Individual Panel in Input Bar
 * @type {*|Object}
 */
GenericAnalysisInputPanel = Ext.extend(Ext.Panel, {

	columnWidth: .25,
	height: 120,
	layout: 'fit',
	notifyFunc: null,
	toolTipTitle : null,
	toolTipTxt: null,

	isDroppable: false, // by default panel is not droppable

	constructor: function(config) {
		GenericAnalysisInputPanel.superclass.constructor.apply(this, arguments);
	},

	listeners: {
		afterLayout : function () { // is triggered when component has been rendered
			if (this.isDroppable) { // apply droppable when it is
				this.applyDroppable(this.notifyFunc);
			}
			// apply tool tip
			if (this.toolTipTxt != null) {
				this.applyToolTip(this.toolTipTitle, this.toolTipTxt);
			}
		}
	},

	// applying droppable
	applyDroppable: function(notifyFunc) {
		var dropTarget = this.getEl().select('.x-panel-bwrap .x-panel-body'); //return array
		var ddTarget = new Ext.dd.DropTarget(dropTarget.elements[0], {
				ddGroup : 'makeQuery',
				notifyDrop : notifyFunc
			}
		);
	},


	applyToolTip: function(title, html) {
		var ttip = new GenericToolTip ({
			target: this.getEl(),
			title: title,
			html: html
		});
	},


	getInputEl: function() {
		return this.getEl().select('.x-panel-bwrap .x-panel-body', true).item(0);
	},

	isEmpty: function() {
		var isEmpty = true;
		if (this.getInputEl().dom.childNodes.length > 0) {
			isEmpty = false;
		}

		return isEmpty;
	},

	getConceptCode: function() {
		var v = getQuerySummaryItem(this.getInputEl().dom.childNodes[0]);
		v = v.trim();
		return v;
	},

	getConceptCodes: function() {
		var v = '';
		var separator = '|';

		nodes = this.getInputEl().dom.childNodes;

		for (var i =0; i<nodes.length; i++ ) {

			var node = getQuerySummaryItem(nodes[i]).trim();

			if (i == nodes.length-1) {
				v = v + node;
			} else {
				v = v + (node + separator);
			}

		}

		return v;

	},

	getNodeList: function() {
		return createNodeTypeArrayFromDiv(this.getInputEl(), "setnodetype");
	}

});

/**
 * Input Bar
 * @type {*|Object}
 */
GenericAnalysisInputBar = Ext.extend(Ext.Panel, {

	layout:'column',
	collapsible: true,

	constructor: function(config) {
		GenericAnalysisInputBar.superclass.constructor.apply(this, arguments);
	},

	init: function () {

	},

	createChildPanel : function(config) {

		var childPanel =  new GenericAnalysisInputPanel({
			title: config.title,
			id: config.id,
			isDroppable: config.isDroppable,
			notifyFunc: config.notifyFunc,
			toolTipTitle: config.toolTipTitle,
			toolTipTxt: config.toolTipTxt
		});

		var _this = this;

		//add tool button if it is droppable
		if (config.isDroppable) {
			childPanel.tools = [{
				id: 'refresh',
				handler: function(e, toolEl, panel, tc){
					_this.clearInput(panel.getId());
				}
			}];
		}

		// add childPanel to input panel
		this.add(childPanel);
		return childPanel;
	},

	createCheckBoxForm: function (checkboxes, id) {
		// define alteration types
		var chkGroup = new Ext.form.CheckboxGroup({
			// Put all controls in a single column with width 75%
			id: id,
			columns: 1,
			height: 75,
			style: 'margin-left: 5px;',
			items: checkboxes
		});

		return chkGroup;
	},

	createRadioBtnGroup: function (radioBtns, id) {
		var radGroup = new Ext.form.RadioGroup({
			id: id,
			columns: 1,
			height: 75,
			vertical: true,
			style: 'margin-left: 5px;',
			items: radioBtns
		});

		return radGroup;
	},

	clearInput: function (panelId) {
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
});

/**
 * Generic tool bar
 * @type {*|Object}
 */
GenericAnalysisToolBar = Ext.extend(Ext.Toolbar, {
	height: 30,
	constructor: function(config) {
		GenericAnalysisInputBar.superclass.constructor.apply(this, arguments);
	}
});

/**
 * Generic result grid panel with grouping feature
 * @type {*|Object}
 */
GenericAnalysisResultGrid = Ext.extend(Ext.grid.GridPanel, {

	frame:true,
	height: 250,
	collapsible: true,
	animCollapse: false,
	iconCls: 'gridbutton',

	constructor: function(config) {
		GenericAnalysisResultGrid.superclass.constructor.apply(this, arguments);
	}

});


/**
 * Panel to display plot of analysis result
 * @type {*|Object}
 */
GenericPlotPanel = Ext.extend(Ext.Panel, {

	constructor: function () {
		GenericPlotPanel.superclass.constructor.apply(this, arguments);
	}

});

/**
 * Tab panel to display survival analysis plots
 * @type {*|Object}
 */
GenericTabPlotPanel = Ext.extend(Ext.TabPanel, {

	width:'100%',
	frame:true,
	height:1000,
	defaults: {autoScroll:true},

	templateId: null,

	constructor: function () {
		GenericTabPlotPanel.superclass.constructor.apply(this, arguments);
	},

	addTab: function (region, tab_id, templateFile, tab_title) {

		var p =  this.findById(tab_id); //find if the plot already displayed

		if (p == null) { // if not yet then create and add a tab
			// create tab item
			p = this.add({
				id: tab_id,
				title: tab_title,
				closable:true
			});
		}

		//set active tab
		this.setActiveTab(p);

		//redo layout
		this.doLayout();

		// generate template with associated region values in selected tab
		templateFile.overwrite(Ext.get(tab_id), region);

	}
});

/**
 * View in most of the analysis
 * @type {*|Object}
 */
GenericAnalysisView = Ext.extend(Object, {

	jobWindow: null, // status window to display job status

	jobTask: null, // task to check job status

	callback: null, // callback from original submit call to be invoked when analysis job is finished

	formParams: null, // form parameters

	subView: null, // subclass of view who invokes the submit job


	reset: function () {

		// destroy jobWindow instance
		Ext.destroy(Ext.get('showJobStatus'));
		this.jobWindow = null;

		// reset current subset IDs
		GLOBAL.CurrentSubsetIDs[1] = null;
		GLOBAL.CurrentSubsetIDs[2] = null;
	},

    /**
     * translate abberation/alteration to readable format
     * TODO: this function can be refactored to be placed in the utils object.
     */
    translateAlteration: function(alt) {

        var v = 'NaN';
        switch (alt)
        {
            case "1":
                v="gain";
                break;
            case "0":
                v="both";
                break;
            case "-1":
                v="loss";
                break;
        }
        return v;
    },

	/**
	 * Submit job defined to the backend
	 * @param formParams
	 * @param callback
	 * @param view
	 * @returns {boolean}
	 */
	submitJob: function (formParams, callback, view) {
		var _parent = this;

		this.callback = callback;
		this.formParams = formParams;
		this.subView = view;

		// reset instances
		this.reset();

		//Make sure at least one subset is filled in.
		if(isSubsetEmpty(1) && isSubsetEmpty(2))
		{
			Ext.Msg.alert('Missing input!','Please select a cohort from the \'Comparison\' tab.');
			return;
		}

		if((!isSubsetEmpty(1) && GLOBAL.CurrentSubsetIDs[1] == null) || (!isSubsetEmpty(2) && GLOBAL.CurrentSubsetIDs[2] == null))
		{

            runAllQueries(function() {
                formParams.result_instance_id1 = getQuerySummary(1);
                formParams.result_instance_id2 = getQuerySummary(2);
				_parent.createJob();
			});
		}

		return true;
	},

	createJob: function () {
		var _parent = this;
		// submit request to create new job
		Ext.Ajax.request({
			url: pageInfo.basePath+"/asyncJob/createnewjob",
			method: 'POST',
			success: function(result, request){
				_parent.executeJob(result);

			},
			failure: function(result, request){
				Ext.Msg.alert('Status', 'Unable to create job.');
			},
			timeout: '1800000',
			params: this.formParams
		});

	},

	executeJob: function (jobInfo) {

		var _parent = this;
		var jobNameInfo = Ext.util.JSON.decode(jobInfo.responseText);

		var jobName = jobNameInfo.jobName;
		setJobNameFromRun(jobName);

		this.formParams.result_instance_id1=GLOBAL.CurrentSubsetIDs[1];
		this.formParams.result_instance_id2=GLOBAL.CurrentSubsetIDs[2];
		this.formParams.analysis=document.getElementById("analysis").value;
		this.formParams.jobName=jobName;

		Ext.Ajax.request(
		{
			url: pageInfo.basePath+"/RModules/scheduleJob",
			method: 'POST',
			timeout: '1800000',
			failure: function (result) {
				Ext.Msg.alert('Status', 'Unable to schedule job.');
			},
			params: Ext.urlEncode(this.formParams) // or a URL encoded string
		});

		_parent.checkPluginJobStatus(jobName);

	},

	//Called to check the heatmap job status
	checkPluginJobStatus: function(jobName)
	{

		var pollInterval = 1000;   // 1 second
		var _me = this;

		this.jobTask =	{
			jobName: jobName,
			parent: this,
			run: function() {
				this.parent.updateJobStatus(this.jobName);
			},
			interval: pollInterval
		}

		Ext.TaskMgr.start(this.jobTask);
	},

	updateJobStatus: function (jobName) {

		var _me = this;

		Ext.Ajax.request(
			{
				url : pageInfo.basePath+"/asyncJob/checkJobStatus",
				method : 'POST',
				success : function(result, request)
				{
					var jobStatusInfo = Ext.util.JSON.decode(result.responseText);
					var status = jobStatusInfo.jobStatus;
					var errorType = jobStatusInfo.errorType;
					var viewerURL = jobStatusInfo.jobViewerURL;
					var altViewerURL = jobStatusInfo.jobAltViewerURL;
					var exception = jobStatusInfo.jobException;
					var resultType = jobStatusInfo.resultType;
					var jobResults = jobStatusInfo.jobResults;

					// show job status
					_me.showJobStatusWindow(status, jobStatusInfo.jobStatusHTML, jobName);

					if (status =='Completed') {

						// close the job window
						_me.jobWindow.close();

						// stop the task manager
						Ext.TaskMgr.stop(_me.jobTask);

						//Set the results DIV to use the URL from the job.
						var fullViewerURL = pageInfo.basePath + viewerURL;
						Ext.get('analysisOutput').load({url : fullViewerURL, callback: loadModuleOutput});

						//Set the flag that says we run an analysis so we can warn the user if they navigate away.
						GLOBAL.AnalysisRun = true;

						// run the callback
						_me.callback(jobName, _me.subView);

					} else if (status == 'Cancelled') {
						Ext.TaskMgr.stop(_me.jobTask);
					} else if (status == 'Error') {
						// close the job window
						_me.jobWindow.close();
						// stop the task
						Ext.TaskMgr.stop(_me.jobTask);
						// inform user on mandatory inputs need to be defined
						Ext.MessageBox.show({
							title: 'Error',
							msg: jobStatusInfo.jobException,
							buttons: Ext.MessageBox.OK,
							icon: Ext.MessageBox.ERROR
						});
					}

					// update work flow status
					updateWorkflowStatus(jobStatusInfo);
				},
				failure : function(result, request)
				{
					Ext.TaskMgr.stop(_me.jobTask);
					showWorkflowStatusErrorDialog('Failed', 'Could not complete the job, please contact an administrator');
				},
				timeout : '300000',
				params: {jobName: jobName}
			}
		);
	},

	showJobStatusWindow: function(status, statusHTML, jobName) {
		var _this = this;

		if (this.jobWindow == null) { // only create status window when it doesn't exist yet

			this.jobWindow = new Ext.Window({
				id: 'showJobStatus',
				title: 'Job Status',
				layout:'fit',
				width:350,
				height:400,
				closable: false,
				plain: true,
				modal: true,
				border:false,
				resizable: false,
				statusMessage: '',
				buttons: [
					{
						text: 'Run Job in Background',
						handler: function()	{
							_this.jobWindow.close();
						}
					},
					{
						text: 'Cancel Job',
						handler: function()	{

							// inform user on mandatory inputs need to be defined
							Ext.MessageBox.show({
								title: 'Cancel Job',
								msg: 'Are you sure you want to cancel your job?',
								buttons: Ext.MessageBox.YESNO,
								icon: Ext.MessageBox.QUESTION,
								fn: function (btn) {
									_this.cancelJobHandler(btn, jobName);
								}
							});

						}
					}],
				html: statusHTML

			});

			// show it
			this.jobWindow.show(viewport);

		}  else {
			// update status
			this.jobWindow.body.update(statusHTML);

		}
	},

	cancelJobHandler: function (btn, jobName) {
		if (btn == 'yes') {

			Ext.Ajax.request(
				{
					url : pageInfo.basePath+"/asyncJob/canceljob",
					method : 'POST',
					timeout : '300000',
					params: {jobName: jobName}
				}
			);

			this.jobWindow.close();
		}
	}

});



