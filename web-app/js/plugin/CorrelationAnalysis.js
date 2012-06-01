function submitCorrelationAnalysisJob(form){
	var variablesConceptCode = readConceptVariables("divVariables");

	var formParams = {	variablesConceptPaths:variablesConceptCode, 
						correlationBy:form.correlationBy.value,
						correlationType:form.correlationType.value
					};

	var variableEle = Ext.get("divVariables");
	
	//If the list of concepts we are running the analysis on is empty, alert the user.
	if(variablesConceptCode == '' || (variableEle.dom.childNodes.length < 2))
	{
		Ext.Msg.alert('Missing input!', 'Please drag at least two concepts into the variables box.');
		return;
	}	
	
	submitJob(formParams);
}

function loadCorrelationAnalysisView(){
	registerCorrelationAnalysisDragAndDrop();
}

function clearGroupCorrelation(divName)
{
	//Clear the drag and drop div.
	var qc = Ext.get(divName);
	
	for(var i=qc.dom.childNodes.length-1;i>=0;i--)
	{
		var child=qc.dom.childNodes[i];
		qc.dom.removeChild(child);
	}	

}

function registerCorrelationAnalysisDragAndDrop()
{
	//Set up drag and drop for Dependent and Independent variables on the data association tab.
	//Get the Dependent DIV.
	var variablesDiv = Ext.get("divVariables");
	
	//Add the drop targets and handler function.
	dtgD = new Ext.dd.DropTarget(variablesDiv,{ddGroup : 'makeQuery'});
	dtgD.notifyDrop =  dropNumericOntoCategorySelection;
}