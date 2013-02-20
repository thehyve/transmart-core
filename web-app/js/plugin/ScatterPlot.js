/*************************************************************************   
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/

function submitScatterPlotJob(form){
	
	var dependentVariableConceptCode = "";
	var independentVariableConceptCode = "";	
	
	dependentVariableConceptCode = readConceptVariables("divDependentVariable");
	independentVariableConceptCode = readConceptVariables("divIndependentVariable");
	
	//------------------------------------
	//Validation
	//------------------------------------
	//Make sure the user entered some items into the variable selection boxes.
	if(dependentVariableConceptCode == '' && independentVariableConceptCode == '')
	{
		Ext.Msg.alert('Missing input', 'Please drag at least one concept into the independent variable and dependent variable boxes.');
		return;
	}
	if(dependentVariableConceptCode == '')
	{
		Ext.Msg.alert('Missing input', 'Please drag at least one concept into the dependent variable box.');
		return;
	}
	if(independentVariableConceptCode == '')
	{
		Ext.Msg.alert('Missing input', 'Please drag at least one concept into the independent variable box.');
		return;
	}
	
	//Loop through the dependent variable box and find the the of nodes in the box.
	var dependentVariableEle = Ext.get("divDependentVariable");
	var independentVariableEle = Ext.get("divIndependentVariable");
	
	var dependentNodeList = createNodeTypeArrayFromDiv(dependentVariableEle,"setnodetype")
	var independentNodeList = createNodeTypeArrayFromDiv(independentVariableEle,"setnodetype")
	
	//The comment section contains trial:TRIALNAME so that we can validate to make sure all the nodes are from the same study.
	var dependentNodeCommentList = createNodeTypeArrayFromDiv(dependentVariableEle,"conceptcomment")
	var independentNodeCommentList = createNodeTypeArrayFromDiv(independentVariableEle,"conceptcomment")
	
	//If the user dragged in multiple node types, throw an error.
	if(dependentNodeList.length > 1)
	{
		Ext.Msg.alert('Wrong input', 'You may only drag nodes of the same type (Continuous,Categorical,High Dimensional) into the input box. The Dependent input box has multiple types.');
		return;		
	}		

	if(independentNodeList.length > 1)
	{
		Ext.Msg.alert('Wrong input', 'You may only drag nodes of the same type (Continuous,Categorical,High Dimensional) into the input box. The Independent input box has multiple types.');
		return;		
	}		
	
	//For the valueicon and hleaficon nodes, you can only put one in a given input box.
	if((dependentNodeList[0] == 'valueicon' || dependentNodeList[0] == 'hleaficon') && (dependentVariableConceptCode.indexOf("|") != -1))
	{
		Ext.Msg.alert('Wrong input', 'For continuous and high dimensional data, you may only drag one node into the input boxes. The Dependent input box has multiple nodes.');
		return;		
	}		

	if((independentNodeList[0] == 'valueicon' || independentNodeList[0] == 'hleaficon') && (independentVariableConceptCode.indexOf("|") != -1))
	{
		Ext.Msg.alert('Wrong input', 'For continuous and high dimensional data, you may only drag one node into the input boxes. The Independent input box has multiple nodes.');
		return;		
	}		
	
	//Nodes will be either 'hleaficon' or 'valueicon'.
	//Scatter plot requires 2 continuous variables.
	var depVariableType = "";
	var indVariableType = "";	
	
	//If there is a categorical variable in either box (This means either of the lists are empty)
	if(!dependentNodeList[0] || dependentNodeList[0] == "null") depVariableType = "CAT";
	if(!independentNodeList[0] || independentNodeList[0] == "null") indVariableType = "CAT";	
	
	//If we have a value icon node, or a high dim that isn't SNP genotype, it is continuous.
	if((dependentNodeList[0] == 'valueicon' || (dependentNodeList[0] == 'hleaficon' && !(window['divDependentVariableSNPType'] == "Genotype" && window['divDependentVariablemarkerType'] == 'SNP')))) depVariableType = "CON";
	if((independentNodeList[0] == 'valueicon' || (independentNodeList[0] == 'hleaficon' && !(window['divIndependentVariableSNPType'] == "Genotype" && window['divIndependentVariablemarkerType'] == 'SNP')))) indVariableType = "CON";
	
	//If we don't have two continuous variables, throw an error.
	if(!(depVariableType=="CON"))
	{
		Ext.Msg.alert('Wrong input', 'Scatter plot requires 2 continuous variables and the dependent variable is not continuous.');
		return;		
	}
	
	if(!(indVariableType=="CON"))
	{
		Ext.Msg.alert('Wrong input', 'Scatter plot requires 2 continuous variables and the independent variable is not continuous.');
		return;		
	}
	
	//------------------------------------	
	
	var logX = form.logX.checked;
	
	var variablesConceptCode = dependentVariableConceptCode+"|"+independentVariableConceptCode;
	
	var formParams = {
			dependentVariable:						dependentVariableConceptCode,
			independentVariable:					independentVariableConceptCode,
			variablesConceptPaths:					variablesConceptCode,
			logX:									logX,
			jobType:								'ScatterPlot'			
	}
	
	if(!loadHighDimensionalParameters(formParams)) return false;
	
	
	//------------------------------------
	//More Validation
	//------------------------------------	
	//If the user dragged in a high dim node, but didn't enter the High Dim Screen, throw an error.
	if(dependentNodeList[0] == 'hleaficon' && formParams["divDependentVariableType"] == "CLINICAL")
	{
		Ext.Msg.alert('Wrong input', 'You dragged a High Dimensional Data node into the dependent variable box but did not select any filters. Please click the "High Dimensional Data" button and select filters. Apply the filters by clicking "Apply Selections".');
		return;			
	}
	if(independentNodeList[0] == 'hleaficon' && formParams["divIndependentVariableType"] == "CLINICAL")
	{
		Ext.Msg.alert('Wrong input', 'You dragged a High Dimensional Data node into the independent variable box but did not select any filters. Please click the "High Dimensional Data" button and select filters. Apply the filters by clicking "Apply Selections".');
		return;			
	}	
	//------------------------------------
	
	submitJob(formParams);
}

/**
 * Register drag and drop.
 * Clear out all gobal variables and reset them to blank.
 */
function loadScatterPlotView(){
	registerScatterPlotDragAndDrop();
	clearGroupScatter('divIndependentVariable');
	clearGroupScatter('divDependentVariable');
	clearHighDimensionalFields();
}

/**
 * Clear the variable selection box
 * Clear all selection stored in global variables
 * Clear the selection display
 * @param divName
 */
function clearGroupScatter(divName)
{
	//Clear the drag and drop div.
	var qc = Ext.get(divName);
	
	for(var i=qc.dom.childNodes.length-1;i>=0;i--)
	{
		var child=qc.dom.childNodes[i];
		qc.dom.removeChild(child);
	}	
	clearHighDimDataSelections(divName);
	clearSummaryDisplay(divName);
}

function registerScatterPlotDragAndDrop()
{
	//Set up drag and drop for Dependent and Independent variables on the data association tab.
	//Get the Dependent DIV.
	var dependentDiv = Ext.get("divDependentVariable");
	//Get the Independent DIV
	var independentDiv = Ext.get("divIndependentVariable");
	
	//Add the drop targets and handler function.
	dtgD = new Ext.dd.DropTarget(dependentDiv,{ddGroup : 'makeQuery'});
	dtgD.notifyDrop =  dropOntoCategorySelection;
	
	dtgI = new Ext.dd.DropTarget(independentDiv,{ddGroup : 'makeQuery'});
	dtgI.notifyDrop =  dropOntoCategorySelection;
	
}