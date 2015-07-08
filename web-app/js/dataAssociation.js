//# sourceURL=dataAssociation.js

Ext.onReady(function(){
    advancedWorkflowMenu();
});

var analysisConcept = null;
var module_array;

function advancedWorkflowMenu() {
    var advancedMenu = null;

    Ext.Ajax.request({
        url : pageInfo.basePath+"/plugin/modules",
        params: {pluginName:'R-Modules'},
        method : 'GET',
        timeout: '1800000',
        success : function(result, request)
    {
        createAdvancedWorkflowMenu(result);
    },
        failure : function(result, request)
    {
        //Ext.Msg.alert("Problem loading the list of available Analysis");
    }
    });
}

/**
 * Find module by module id
 * @param id
 * @returns {null}
 */
function findModuleById(id) {
    var v = null;
    if (module_array != null) {
        Ext.each(module_array, function (module, index) {
            if (module.id == id) {
                v = module;
                return false;
            }
        });
    }
    return v;
}

function createAdvancedWorkflowMenu(result) {
    var response = Ext.util.JSON.decode(result.responseText);
    if (response.success) {

        // reserve modules in a global var
        module_array = response.modules;

        var advMenuItems = createAdvancedWorkflowMenuItems(response.modules);
        var advMenu = new Ext.menu.Menu({
            id : 'advancedWorkflowMenu',
            minWidth: 250,
            items : advMenuItems
        });

        // add components to the advanced workflow toolbar
        Ext.getCmp('advancedWorkflowToolbar')
            .add(
                    {
                        text : 'Analysis',
                iconCls : 'comparebutton',
                disabled : false,
                menu : advMenu
                    }
//                ,'->',
//                    {
//                        text : 'Save to PDF',
//                iconCls : 'savepdfbutton',
//                hidden : false,
//                id : 'savetopdfbtn',
//                handler: function(){
//                    generatePdfFromHTML('dataAssociationBody', 'DataAssociation.pdf');
//                }
//                    }
                );
    }
}

function createAdvancedWorkflowMenuItems(modules) {
    var menuItems = [];
    Ext.each(modules, function(module, index) {
        var menuItem = module;
        menuItem.handler = onItemClick;

        menuItems.push(menuItem);
    });

    return menuItems;
}

/**
 * This function will load the analysis page
 * @param itemId
 * @param isCompletedJob
 * @param jobName
 */
function loadAnalysisPage(itemId, isCompletedJob, jobName) {

    // get analysis module attribute
    var module = findModuleById(itemId);

    // translate group test module name ..
    // TODO: Please change with the consistent naming for all related files and variable
    if (itemId ==  'aCGHgroupTest' ) itemId = 'groupTestaCGH';
    if (itemId ==  'RNASeqgroupTest' ) itemId = 'groupTestRNASeq';

    // ajax call to
    $j.ajax({
        url : pageInfo.basePath+'/dataAssociation/variableSelection',
        data : {analysis:itemId},
        success : function (response, status) {

            // insert response into 'variableSelection' html element
            $j('#variableSelection').html(response);

            // load the plugin view
            loadPluginView(itemId);

            // if it's loading completed job then display the result as well
            if (isCompletedJob) {
                switch (itemId) {
                    case 'aCGHSurvivalAnalysis' :
                        survivalAnalysisACGHView.renderResults(jobName, survivalAnalysisACGHView);
                        break;
                    case 'groupTestaCGH' :
                        groupTestView.renderResults(jobName, groupTestView);
                        break;
                    case 'groupTestRNASeq' :
                        RNASeqgroupTestView.renderResults(jobName, RNASeqgroupTestView);
                        break;
                    case 'acghFrequencyPlot' :
                        frequencyPlotView.renderResults(jobName, frequencyPlotView);
                        break;
                }
            }
        }
    }),

        // update analysis element
        Ext.get('analysis').dom.value = itemId;
    if (module) {
        Ext.fly('selectedAnalysis').update(module.text, false).removeClass('warning').addClass('selected');
    }
}

function onItemClick(item) {
    //Ext.Msg.alert('Menu Click', 'You clicked the menu item '+item.text);

    if(!checkPreviousAnalysis()) return false;

    loadAnalysisPage(item.id);

    item.parentMenu.hide(true);
    /*var mgr = Ext.Updater('variableSelection')

      mgr.update({
      url : pageInfo.basePath+"/dataAssociation/variableSelection",
      params: {analysis:item.id},
      method : 'GET',
      timeout: '1800000',
      success : function(result, request) {
      loadPluginView(item.id);
      },
      failure : function(result, request) {
    //Ext.Msg.alert("Problem loading the list of available Analysis");
    }
    });*/
}
function renderCohortSummary(){
    var cohortsSummary=""

        // get selected cohort summary
        for(var i = 1; i<=GLOBAL.NumOfSubsets; i++){
            var currentQuery = getQuerySummary(i)
                if(currentQuery!=""){
                    cohortsSummary += "Subset "+i+": "
                        cohortsSummary += currentQuery
                        cohortsSummary += "<br>"
                }

        }

    if(""==cohortsSummary){
        // hide cohort Summary & show warning
        Ext.get('cohortSummary').hide();
        Ext.get('cohortWarningMsg').show();
        Ext.fly('cohortWarningMsg').update("WARNING: You have not selected a study and the analysis will not work. " +
                "Please go back to the Comparison tab and make a cohort selection.").addClass("warning");
    } else {
        // hide warning & show cohort Summary
        Ext.fly('cohortSummary').update(cohortsSummary);
        Ext.get('cohortWarningMsg').hide();
        Ext.get('cohortSummary').show();
    }


}

function checkPreviousAnalysis()
{
    //If the user clicks submit but they've run a analysis recently check with them to make sure they want to clear the results.
    if(GLOBAL.AnalysisRun)
    {
        return confirm('When you navigate to a new analysis the current analysis results will be cleared! If you would like your results to be saved click the "Save to PDF" button. Are you sure you wish to navigate away?');
    }

    return true;
}


function loadPluginView(){

    //Remove the output screen.
    document.getElementById("analysisOutput").innerHTML = "";

    //Whenever we switch views, make the binning toggle false. All the analysis pages default to this state.
    GLOBAL.Binning = false
        GLOBAL.ManualBinning = false
        GLOBAL.NumberOfBins = 4
        GLOBAL.AnalysisRun = false

        var selectedAnalysis = document.getElementById("analysis").value;
    selectedAnalysis = selectedAnalysis.charAt(0).toUpperCase()+selectedAnalysis.substring(1);
    eval("load"+selectedAnalysis+"View()");

}

//This function fires when an item is dropped onto one of the independent/dependent variable DIVs in the data association tool.
function dropOntoVariableSelection(source, e, data)
{
    data.node.attributes.oktousevalues = "N"
        var concept = createPanelItemNew(this.el, convertNodeToConcept(data.node));
    return true;
}

//This function fires when an item is dropped onto one of the
//independent/dependent variable DIVs in the data association tool.
//Used to ensure only a numeric value is dropped. For all values use dropOntoCategorySelection function
function dropNumericOntoCategorySelection(source, e, data){
    var targetdiv=this.el;
    if(data.node.leaf==false && !data.node.isLoaded()){
        data.node.reload(function(){dropNumericOntoCategorySelection2(source, e, data, targetdiv);});
    }
    else{
        dropNumericOntoCategorySelection2(source, e, data, targetdiv);
    }
    return true;
}

function dropNumericOntoCategorySelection2(source, e, data, targetdiv)
{
    //Node must be folder so use children leafs
    if(data.node.leaf==false)
    {
        //Keep track of whether all the nodes are numeric or not.
        var allNodesNumeric = true

            //Keep track of whether the folder has any leaves.
            var foundLeafNode = false

            //Loop through child nodes to add them to input.
            for ( var i = 0; i<data.node.childNodes.length; i++)
            {
                //Grab the child node.
                var child=data.node.childNodes[i];

                //This tells us whether it is a numeric or character node.
                var val=child.attributes.oktousevalues;

                //If we are a numeric leaf node, add it to the tree.
                if(val==='Y' && child.leaf==true)
                {
                    //Reset the alpha/numeric flag so we don't get the popup for entering a value.
                    child.attributes.oktousevalues = "N";

                    //Set the flag indicating we had a leaf node.
                    foundLeafNode = true;

                    //Add the item to the input.
                    var concept = createPanelItemNew(targetdiv, convertNodeToConcept(child));

                    //Set back to original value
                    child.attributes.oktousevalues=val;
                }
                else if(val==='N' && child.leaf==true)
                {
                    //Set the flag indicating we had a leaf node.
                    foundLeafNode = true;

                    //If we find a non-numeric node, set our flag.
                    allNodesNumeric = false
                }

            }

        //If no leaf nodes found, alert the user.
        if(!foundLeafNode)
        {
            Ext.Msg.alert('No Nodes in Folder','When dragging in a folder you must select a folder that has leaf nodes directly under it.');
        }

        //If we found a non numeric node, alert the user.
        if(!allNodesNumeric && foundLeafNode)
        {
            Ext.Msg.alert('Numeric Input Required','Please select numeric concepts only for this input. Numeric concepts are labeled with a "123" in the tree.');
        }
    }
    else
    {
        //If we dragged a numeric leaf, add it to the input. Otherwise alert the user.
        if(data.node.attributes.oktousevalues==='Y')
        {
            //This tells us whether it is a numeric or character node.
            var val=data.node.attributes.oktousevalues;

            //Reset the alpha/numeric flag so we don't get the popup for entering a value.
            data.node.attributes.oktousevalues="N";

            //Add the item to the input.
            var concept = createPanelItemNew(targetdiv, convertNodeToConcept(data.node));

            //Set back to original value
            data.node.attributes.oktousevalues=val;
        }
        else
        {
            Ext.Msg.alert('Numeric Input Required','Please select numeric concepts only for this input. Numeric concepts are labeled with a "123" in the tree.');
        }
    }
    return true;
}


//This function fires when an item is dropped onto one of the
//independent/dependent variable DIVs in the data association tool.
function dropOntoCategorySelection(source, e, data)
{
    var targetdiv=this.el;

    if(data.node.leaf==false && !data.node.isLoaded())
    {
        data.node.reload(function(){
            analysisConcept = dropOntoCategorySelection2(source, e, data, targetdiv);
        });
    }
    else
    {
        analysisConcept = dropOntoCategorySelection2(source, e, data, targetdiv);
    }
    return true;
}

function dropOntoCategorySelection2(source, e, data, targetdiv)
{
    //Node must be folder so use children leafs
    if(data.node.leaf==false)
    {

        //Keep track of whether the folder has any leaves.
        var foundLeafNode = false

            for ( var i = 0; i<data.node.childNodes.length; i++)
            {
                //Grab the child node.
                var child=data.node.childNodes[i];

                //If this is a leaf node, add it.
                if(child.leaf==true)
                {
                    //Add the item to the input.
                    var concept = createPanelItemNew(targetdiv, convertNodeToConcept(child));

                    //Set the flag indicating we had a leaf node.
                    foundLeafNode = true;
                }
            }
        //Adding this condition for certain nodes like Dosage and Response, where children of Dosage & Response are intentionally hidden
        if (data.node.childrenRendered && data.node.firstChild == null) {
            foundLeafNode = true;
            var concept = createPanelItemNew(targetdiv, convertNodeToConcept(data.node));
        }

        //If no leaf nodes found, alert the user.
        if(!foundLeafNode)
        {
            Ext.Msg.alert('No Nodes in Folder','When dragging in a folder you must select a folder that has leaf nodes directly under it.');
        }
    } else {
        //Add the item to the input.
        var concept = createPanelItemNew(targetdiv, convertNodeToConcept(data.node));
    }
    return concept;
}

//This function will create an array of all the node types from a box that i2b2 nodes were dragged into.
function createNodeTypeArrayFromDiv(divElement, attributeToPull) {
    var nodeTypeList = [];

    //If the category variable element has children, we need to parse them and add their values to an array.
    if(divElement.dom.childNodes[0]) {

        //Loop through the category variables and add them to a comma seperated list.
        for(nodeIndex = 0; nodeIndex < divElement.dom.childNodes.length; nodeIndex++) {
            var currentNode = divElement.dom.childNodes[nodeIndex];
            var currentNodeType = currentNode.attributes.getNamedItem(attributeToPull).value;

            //If we find an item, add it to the array.
            if(currentNodeType) {
                nodeTypeList.push(currentNodeType.toString());
            }
        }
    }

    //Make the elements in the array unique.
    return nodeTypeList.unique();
}

//This might be inefficient.
//Return new array with duplicate values removed
Array.prototype.unique =
function() {
    var a = [];
    var l = this.length;
    for(var i=0; i<l; i++) {
        for(var j=i+1; j<l; j++) {
            // If this[i] is found later in the array
            if (this[i] === this[j])
                j = ++i;
        }
        a.push(this[i]);
    }
    return a;
};


function setupSubsetIds(formParams){

    runAllQueries(function(){submitJob(formParams);});

}

function readConceptVariables(divIds){
    var variableConceptPath = ""
    var variableEle = Ext.get(divIds);

    //If the variable element has children, we need to parse them and concatenate their values.
    if (variableEle && variableEle.dom.childNodes[0]) {
        //Loop through the variables and add them to a comma separated list.
        for(nodeIndex = 0; nodeIndex < variableEle.dom.childNodes.length; nodeIndex++) {
            //If we already have a value, add the separator.
            if (variableConceptPath != '') {
                variableConceptPath += '|'
            }

            //Add the concept path to the string.
            variableConceptPath += RmodulesView.fetch_concept_path(
                variableEle.dom.childNodes[nodeIndex])
        }
    }
    return variableConceptPath;
}

function submitJob(formParams)
{
    //Make sure at least one subset is filled in.
    if(isSubsetEmpty(1) && isSubsetEmpty(2))
    {
        Ext.Msg.alert('Missing input!','Please select a cohort from the \'Comparison\' tab.');
        return;
    }

    if((!isSubsetEmpty(1) && GLOBAL.CurrentSubsetIDs[1] == null) || (!isSubsetEmpty(2) && GLOBAL.CurrentSubsetIDs[2] == null))
    {
        setupSubsetIds(formParams);
        return;
    }
    createWorkflowStatus($j('#dataAssociationBody'), true);

    Ext.Ajax.request({
        url: pageInfo.basePath+"/asyncJob/createnewjob",
        method: 'POST',
        success: function(result, request){
            //Handle data export process
            runJob(result, formParams);
        },
        failure: function(result, request){
                     Ext.Msg.alert('Status', 'Unable to create data export job.');
                 },
        timeout: '1800000',
        params: formParams
    });

}

function runJob(result, formParams) {
    var jobNameInfo = Ext.util.JSON.decode(result.responseText);
    var jobName = jobNameInfo.jobName;
    setJobNameFromRun(jobName);

    formParams.result_instance_id1=GLOBAL.CurrentSubsetIDs[1];
    formParams.result_instance_id2=GLOBAL.CurrentSubsetIDs[2];
    formParams.analysis=document.getElementById("analysis").value;
    formParams.jobName=jobName;

    Ext.Ajax.request(
            {
                url: pageInfo.basePath+"/RModules/scheduleJob",
        method: 'POST',
        timeout: '1800000',
        params: Ext.urlEncode(formParams) // or a URL encoded string
            });

    //Start the js code to check the job status so we can display results when we are done.
    checkPluginJobStatus(jobName)
}

function waitWindowForAnalysis()
{
    //Mask the panel while the analysis runs.
    Ext.getCmp('dataAssociationPanel').body.mask("Running analysis...", 'x-mask-loading');
}

/**
 * A scheduler to check the job status of a job. When the job status completed, cancelled or error it should stop and
 * update the workflow status
 * @param jobName
 */
function checkPluginJobStatus(jobName)
{
    var checkTask = {
        run: function () {
                 Ext.Ajax.request({
                     url : pageInfo.basePath+"/asyncJob/checkJobStatus",
                 method : 'POST',
                 timeout : '300000',
                 params: {jobName: jobName},
                 scope: this,
                 success : function (result, options) {
                     var jobStatusInfo = Ext.util.JSON.decode(result.responseText);
                     var status = jobStatusInfo.jobStatus;
                     var viewerURL = jobStatusInfo.jobViewerURL;
                     var fullViewerURL = pageInfo.basePath + viewerURL;

                     if (status =='Completed') {
                         runner.stopAll();
                         //Set the results DIV to use the URL from the job.
                         Ext.get('analysisOutput').load({url : fullViewerURL, callback: loadModuleOutput});
                         //Set the flag that says we run an analysis so we can warn the user if they navigate away.
                         GLOBAL.AnalysisRun = true;
                     } else if (status == 'Cancelled' || status == 'Error') {
                         runner.stopAll();
                     }
                     updateWorkflowStatus(jobStatusInfo);
                 },
                 failure : function () {
                               runner.stopAll();
                               showWorkflowStatusErrorDialog('Failed', 'Could not complete the job, please contact an ' +
                                       'administrator');
                           }
                 });
             },
        interval: 1000
    }

    //
    var runner = new Ext.util.TaskRunner();  // define a runner
    runner.start(checkTask); // start the task
}

function loadModuleOutput()
{
    var selectedAnalysis = document.getElementById("analysis").value;
    selectedAnalysis = selectedAnalysis.charAt(0).toUpperCase()+selectedAnalysis.substring(1);

    var funcName = "load"+selectedAnalysis+"Output";

    if (typeof funcName == 'string' && eval('typeof ' + funcName) == 'function')
    {
        eval(funcName+'()');
    }
}

// TODO : To be moved as HighDimensional function
function setupCategoricalItemsList (strDivSource, strDivTarget) {
    // copy from the category div at top of page first and add drag handlers
    var categoricalSourceDiv = Ext.get(strDivSource);
    var categoricalTargetDiv = Ext.get(strDivTarget);

    var _dropOntoBin = function (source, e, data) {
        this.el.appendChild(data.ddel);
        return true;
    }

    // clear it out first
    while (categoricalTargetDiv.dom.hasChildNodes())
        categoricalTargetDiv.dom
            .removeChild(categoricalTargetDiv.dom.firstChild);
    for ( var i = 0, n = categoricalSourceDiv.dom.childNodes.length; i < n; ++i) {
        // clone and append
        var newnode = categoricalSourceDiv.dom.childNodes[i].cloneNode(true);
        categoricalTargetDiv.dom.appendChild(newnode);
        // add drag handler
        Ext.dd.Registry.register(newnode, {
            el : newnode
        });
    }
    var dragZone = new Ext.dd.DragZone(categoricalTargetDiv.dom.parentNode, {
        ddGroup : 'makeBin',
        isTarget: true,
        ignoreSelf: false
    });

    var dropZone = new Ext.dd.DropTarget(categoricalTargetDiv, {
        ddGroup : 'makeBin',
        isTarget: true,
        ignoreSelf: false,
        onNodeEnter: function(target, dd, e, dragData) {
            delete this.dropOK;
            this.dropOK=true;
            return true;

        },
        onNodeOver: function(target, dd, e, dragData) {
                        var ret= this.dropOK ? this.dropAllowed : this.dropNotAllowed;
                        console.log(ret);
                        return ret;
                    }
    });
    dropZone.notifyDrop = _dropOntoBin;
}

function clearDataAssociation()
{
    //Remove the output screen.
    document.getElementById("analysisOutput").innerHTML = "";
    //Remove the variable selection screen.
    document.getElementById("variableSelection").innerHTML = "";

    //Whenever we switch views, make the binning toggle false. All the analysis pages default to this state.
    GLOBAL.Binning = false
        GLOBAL.ManualBinning = false
        GLOBAL.NumberOfBins = 4
        GLOBAL.AnalysisRun = false

        //Set the message below the cohort summary that lets the user know they need to select a cohort.
        renderCohortSummary();

}

function loadCommonHighDimFormObjects(formParams, divName)
{
    formParams[divName + "timepoints"]      = window[divName + 'timepoints1'];
    formParams[divName + "samples"]       = window[divName + 'samples1'];
    formParams[divName + "rbmPanels"]     = window[divName + 'rbmPanels1'];
    formParams[divName + "platforms"]     = window[divName + 'platforms1'];
    formParams[divName + "gpls"]        = window[divName + 'gpls1'];
    formParams[divName + "gplsValue"]     = window[divName + 'gplsValue1'];
    formParams[divName + "tissues"]       = window[divName + 'tissues1'];

    formParams[divName + "timepoints2"]     = window[divName + 'timepoints2'];
    formParams[divName + "samples2"]      = window[divName + 'samples2'];
    formParams[divName + "rbmPanels2"]      = window[divName + 'rbmPanels2'];
    formParams[divName + "platforms2"]      = window[divName + 'platforms2'];
    formParams[divName + "gpls2"]       = window[divName + 'gpls2'];
    formParams[divName + "gplsValue2"]      = window[divName + 'gplsValue2'];
    formParams[divName + "tissues2"]      = window[divName + 'tissues2'];

    formParams[divName + "probesAggregation"] = window[divName + 'probesAggregation'];
    formParams[divName + "SNPType"]       = window[divName + 'SNPType'];
    formParams[divName + "PathwayName"]     = window[divName + 'pathwayName'];

    var mrnaData = false
        var mirnaData = false
        var snpData = false

        var tempGeneList    = window[divName + 'pathway'];
    var tempMarkerType    = window[divName + 'markerType'];
    var tempGPL       = window[divName + 'gplValues'];

    var tempPlatform    = window[divName + 'platforms1'] + "," + window[divName + 'platforms2'];
    var tempSampleType    = window[divName + 'samplesValues'];
    var tempTissueType    = window[divName + 'tissuesValues'];
    var tempTime      = window[divName + 'timepointsValues'];

    //If we are using High Dimensional data we need to create variables that represent genes from both independent
    //and dependent selections (In the event they are both of a single high dimensional type).
    //Check to see if the user selected GEX in the independent input.
    if(tempMarkerType == "Gene Expression")
    {
        //The genes entered into the search box were GEX genes.
        var fullGEXGeneList   = tempGeneList;
        var fullGEXSampleType   = String(tempSampleType);
        var fullGEXTissueType   = String(tempTissueType);
        var fullGEXTime     = String(tempTime);
        var fullGEXGPL      = String(tempGPL);

        if(fullGEXSampleType == ",")  fullGEXSampleType = ""
            if(fullGEXTissueType == ",")  fullGEXTissueType = ""
                if(fullGEXTime == ",")      fullGEXTime = ""
                    if(fullGEXGPL == ",")       fullGEXGPL = ""

                        //This flag will tell us to write the GEX text file.
                        mrnaData = true;

        //Fix the platform to be something the R script expects.
        //tempMarkerType = "MRNA"; // after this is commented out it seems does not affect the R script, however marker
        // type needs to be aligned with what has been defined in the gpl info table

        formParams["gexpathway"]                = fullGEXGeneList;
        formParams["gextime"]                 = fullGEXTime;
        formParams["gextissue"]                 = fullGEXTissueType;
        formParams["gexsample"]                 = fullGEXSampleType;
        formParams["gexgpl"]                  = fullGEXGPL;

        formParams["mrnaData"]                  = mrnaData;
    }

    //Check to see if the user selected SNP in the temp input.
    if(tempMarkerType == "SNP")
    {
        //The genes entered into the search box were SNP genes.
        var fullSNPGeneList   = tempGeneList;
        var fullSNPSampleType   = String(tempSampleType);
        var fullSNPTissueType   = String(tempTissueType);
        var fullSNPTime     = String(tempTime);
        var fullSNPGPL      = String(tempGPL);

        if(fullSNPSampleType == ",")  fullSNPSampleType = ""
            if(fullSNPTissueType == ",")  fullSNPTissueType = ""
                if(fullSNPTime == ",")      fullSNPTime = ""
                    if(fullSNPGPL == ",")       fullSNPGPL = ""

                        //This flag will tell us to write the SNP text file.
                        snpData = true;

        formParams["snppathway"]                = fullSNPGeneList;
        formParams["snptime"]                 = fullSNPTime;
        formParams["snptissue"]                 = fullSNPTissueType;
        formParams["snpsample"]                 = fullSNPSampleType;
        formParams["snpgpl"]                  = fullSNPGPL;
        formParams["snpData"]                 = snpData;
    }

    if(tempMarkerType == "QPCR MIRNA")
    {
        //This flag will tell us to write the GEX text file.
        mirnaData = true;
        formParams["mirnaData"]                 = mrnaData;
    }

    //If we don't have a platform, fill in Clinical.
    if(tempPlatform == null || tempPlatform == "") tempMarkerType = "CLINICAL"

        formParams[divName + "Type"]              = tempMarkerType;
    formParams[divName + "Pathway"]             = tempGeneList;
}

