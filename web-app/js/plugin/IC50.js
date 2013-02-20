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

function submitIC50Job(form){
	
	var cellLineVariableConceptCode = "";
	var dosageVariableConceptCode 	= "";
	var responseVariableConceptCode = "";
	
	cellLineVariableConceptCode 		= readConceptVariables("divCellLinesVariable");
	concentrationVariableConceptCode 	= readConceptVariables("divConcentrationVariable");

	var cellLineVariableEle 		= Ext.get("divCellLinesVariable");
	var concentrationVariableEle	= Ext.get("divConcentrationVariable");
	
	var variablesConceptCode = cellLineVariableConceptCode+"|"+concentrationVariableConceptCode;
	
	var cellLineType 		= "CLINICAL"
	var concentrationType 	= "CLINICAL"
	
	//------------------------------------
	//Validation
	//------------------------------------
	//Make sure the user entered some items into the variable selection boxes.
	if(cellLineVariableConceptCode == '')
	{
		Ext.Msg.alert('Missing input', 'Please drag at least one concept into the Cell Line variable box.');
		return;
	}
	
	if(concentrationVariableConceptCode == '')
	{
		Ext.Msg.alert('Missing input', 'Please drag at least one concept into the Concentration variable box.');
		return;
	}	
	
	var cellNodeList 			= createNodeTypeArrayFromDiv(cellLineVariableEle,"setnodetype")
	var concentrationNodeList 	= createNodeTypeArrayFromDiv(concentrationVariableEle,"setnodetype")
	
	//If the user dragged in multiple node types, throw an error.
	if(cellNodeList.length > 1)
	{
		Ext.Msg.alert('Wrong input', 'You may only drag nodes of the same type (Continuous,Categorical,High Dimensional) into the input box. The Cell Line input box has multiple types.');
		return;		
	}			

	if(concentrationNodeList.length > 1)
	{
		Ext.Msg.alert('Wrong input', 'You may only drag nodes of the same type (Continuous,Categorical,High Dimensional) into the input box. The Concentration input box has multiple types.');
		return;		
	}	
	
	//If something was entered into the cell variable box, but we have something in the node list, that means the item dragged in wasn't categorical.
	if(!(!cellNodeList[0] || cellNodeList[0] == "null"))
	{
		Ext.Msg.alert('Wrong input', 'You may only use categorical variables in the Cell Line input box.');
		return;		
	}
	
	//If we didn't drag a continuous node into the Dosage/Response boxes.
	//Commenting them out as the user wants to drag the Dosage / Response node itself and not see all its child nodes in the Dosage / Response input boxes.
	/*if(dosageNodeList[0] != 'valueicon')
	{
		Ext.Msg.alert('Wrong input', 'You may only use continuous variables in the Dosage input box.');
		return;		
	}
	if(responseNodeList[0] != 'valueicon')
	{
		Ext.Msg.alert('Wrong input', 'You may only use continuous variables in the Response input box.');
		return;		
	}
	
	if(dosageVariableConceptCode.indexOf("|") == -1)
	{
		Ext.Msg.alert('Wrong input', 'You must enter at least two dosage data points.');
		return;				
	}
	
	if(responseVariableConceptCode.indexOf("|") == -1)
	{
		Ext.Msg.alert('Wrong input', 'You must enter at least two response data points.');
		return;				
	}*/	
	
	//------------------------------------
	
	var formParams = {
			cellLineVariable:						cellLineVariableConceptCode,
			concentrationVariable:					concentrationVariableConceptCode,
			variablesConceptPaths:					variablesConceptCode,
			jobType:								'IC50',
			parentNodeList:							'concentrationVariable',
			includeContexts:						'true'
	};
	
	submitJob(formParams);
}

/**
 * Register drag and drop.
 * Clear out all gobal variables and reset them to blank.
 */
function loadIc50View(){
	registerIC50DragAndDrop();
	clearHighDimDataSelections('divCellLinesVariable');
	clearHighDimDataSelections('divConcentrationVariable');
}

/**
 * Clear the variable selection box
 * Clear all selection stored in global variables
 * Clear the selection display
 * @param divName
 */
function clearGroupIC50(divName)
{
	//Clear the drag and drop div.
	var qc = Ext.get(divName);
	
	for(var i=qc.dom.childNodes.length-1;i>=0;i--)
	{
		var child=qc.dom.childNodes[i];
		qc.dom.removeChild(child);
	}	
	//clearHighDimDataSelections(divName);
	//clearSummaryDisplay(divName);
}

function registerIC50DragAndDrop()
{
	//Set up the drop targets for the IC50 Analysis.
	
	//Cell Line input.
	dtgC = new Ext.dd.DropTarget(Ext.get("divCellLinesVariable"),{ddGroup : 'makeQuery'});
	dtgC.notifyDrop =  dropOntoCategorySelection;
	
	//Dosage input.
	dtgD = new Ext.dd.DropTarget(Ext.get("divConcentrationVariable"),{ddGroup : 'makeQuery'});
	dtgD.notifyDrop =  dropOntoCategorySelection;
	
}