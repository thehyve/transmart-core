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

function submitWaterfallJob(form){
	
	var dataNodeConceptCode = "";
	
	dataNodeConceptCode = readConceptVariables("divDataNode");

	var variablesConceptCode = dataNodeConceptCode;
	
	var mrnaData = false
	var snpData = false
	
	var fullGEXGeneList = "";
	var fullSNPGeneList = "";
	
	var dataNodeGeneList 	= window['divDataNodepathway'];
	var dataNodePlatform 	= window['divDataNodeplatforms'];
	var dataNodeType 		= window['divDataNodemarkerType'];
	
	var lowRangeOperator	= document.getElementById('selLowRange').value
	var highRangeOperator	= document.getElementById('selHighRange').value
		
	var lowRangeValue		= document.getElementById('txtLowRange').value
	var highRangeValue		= document.getElementById('txtHighRange').value
	
	//If we are using High Dimensional data we need to create variables that represent genes.
	if(dataNodeType == "Gene Expression")
	{
		//If the gene list already has items, add a comma.
		if(fullGEXGeneList != "") fullGEXGeneList += ","
		
		//Add the genes in the list to the full list of GEX genes.
		fullGEXGeneList += dataNodeGeneList
		
		//This flag will tell us to write the GEX text file.		
		mrnaData = true;
		
		//Fix the platform to be something the R script expects.
		dataNodeType = "MRNA";
	}
	
	if(dataNodeType == "SNP")
	{
		//If the gene list already has items, add a comma.
		if(fullSNPGeneList != "") fullGEXGeneList += ","
		
		//Add the genes in the list to the full list of SNP genes.
		fullSNPGeneList += dataNodeGeneList
		
		//This flag will tell us to write the SNP text file.		
		snpData = true;
	}	
	
	
	//----------------------------------
	//Validation
	//----------------------------------
	//This is the independent variable.
	var dataNodeVariableEle = Ext.get("divDataNode");	
	
	//Get the types of nodes from the input box.
	var dataNodeList = createNodeTypeArrayFromDiv(dataNodeVariableEle,"setnodetype")	
	
	//Validate to make sure a concept was dragged in.
	if(dataNodeConceptCode == '')
	{
		Ext.Msg.alert('Missing input', 'Please drag at least one concept into the "Data Node" box.');
		return;
	}		
	
	if((dataNodeList[0] == 'valueicon' || dataNodeList[0] == 'hleaficon') && (dataNodeConceptCode.indexOf("|") != -1))
	{
		Ext.Msg.alert('Wrong input', 'For continuous data, you may only drag one node into the input boxes. The "Data Node" input box has multiple nodes.');
		return;		
	}			
	
	//If there is a value in the low bin field, make sure it is parsable as a number.
	if(lowRangeValue != '' && !isNumberFloat(lowRangeValue))
	{
		Ext.Msg.alert('Wrong input', 'The low range box needs to be left left blank or contain a valid number.');
		return;			
	}
	
	//If there is a value in the low bin field, make sure it is parsable as a number.
	if(highRangeValue != '' && !isNumberFloat(highRangeValue))
	{
		Ext.Msg.alert('Wrong input', 'The high range box needs to be left left blank or contain a valid number.');
		return;			
	}	
	
	if(fullGEXGeneList == "" && dataNodeType == "MRNA")
	{
		Ext.Msg.alert("No Genes Selected!", "Please specify Genes in the Gene/Pathway Search box.")
		return false;
	}
	
	if(fullSNPGeneList == "" && dataNodeType == "SNP")
	{
		Ext.Msg.alert("No Genes Selected!", "Please specify Genes in the Gene/Pathway Search box.")
		return false;
	}	
	//----------------------------------
	
	//If we don't have a platform, fill in Clinical.
	if(dataNodePlatform == null || dataNodePlatform == "") dataNodeType = "CLINICAL"
	
	var formParams = {
			dataNode:						dataNodeConceptCode,
			divDataNodetimepoints:			window['divDataNodetimepoints'],
			divDataNodesamples:				window['divDataNodesamples'],
			divDataNoderbmPanels:			window['divDataNoderbmPanels'],
			divDataNodeplatforms:			dataNodePlatform,
			divDataNodegpls:				window['divDataNodegplsValue'],
			divDataNodetissues:				window['divDataNodetissues'],
			divDataNodeprobesAggregation:	window['divDataNodeprobesAggregation'],
			divDataNodeSNPType:				window['divDataNodeSNPType'],
			divDataNodeType:				dataNodeType,
			divDataNodePathway:				dataNodeGeneList,
			gexpathway:						fullGEXGeneList,
			snppathway:						fullSNPGeneList,
			divDataNodePathwayName:			window['divDataNodepathwayName'],
			mrnaData:						mrnaData,
			snpData:						snpData,
			variablesConceptPaths:			variablesConceptCode,
			lowRangeOperator:				lowRangeOperator,
			highRangeOperator:				highRangeOperator,
			lowRangeValue:					lowRangeValue,
			highRangeValue:					highRangeValue,
			jobType:						'Waterfall'
	};
	
	submitJob(formParams);
}

function isNumberFloat(n) {
	  return !isNaN(parseFloat(n)) && isFinite(n);
	}		

/**
 * Register drag and drop.
 * Clear out all gobal variables and reset them to blank.
 */
function loadWaterfallView(){
	registerWaterfallDragAndDrop();
	clearHighDimDataSelections('divDataNode');
}

/**
 * Clear the variable selection box
 * Clear all selection stored in global variables
 * Clear the selection display
 * @param divName
 */
function clearGroupWaterfall(divName)
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

function registerWaterfallDragAndDrop()
{
	//Set up drag and drop for Dependent and Independent variables on the data association tab.
	//Get the Dependent DIV.
	var dataNodeDiv = Ext.get("divDataNode");
	
	//Add the drop targets and handler function.
	dtgD = new Ext.dd.DropTarget(dataNodeDiv,{ddGroup : 'makeQuery'});
	dtgD.notifyDrop =  dropOntoCategorySelection;
	
}

function getSelRangeValueAsString(selRangeValue) {
	var selRangeStr = null
	if (selRangeValue == '<') selRangeStr = 'LT'
	else if (selRangeValue == '<=') selRangeStr = 'LE'
	else if (selRangeValue == '=') selRangeStr = 'EQ'
	else if (selRangeValue == '>') selRangeStr = 'GT'
	else if (selRangeValue == '>=') selRangeStr = 'GE'
	
	return selRangeStr
}

function selectInputsAsCohort(form) {
	for(var s=1;s<=GLOBAL.NumOfSubsets;s++)
	{
		for(var d=1;d<=GLOBAL.NumOfQueryCriteriaGroups;d++)
		{
			var qcd=Ext.get("queryCriteriaDiv"+s+'_'+d.toString());
			if (s == 1 && qcd.dom.childNodes.length == 0) {
				var lowRangeConcept = createPanelItemNew(qcd, analysisConcept.concept);
				setValue(lowRangeConcept, 'numeric', getSelRangeValueAsString(form.selLowRange.value), 'N', '', form.txtLowRange.value, 'ratio');
				break;
			} else if (s == 2 && qcd.dom.childNodes.length == 0) {
				var highRangeConcept = createPanelItemNew(qcd, analysisConcept.concept);
				setValue(highRangeConcept, 'numeric', getSelRangeValueAsString(form.selHighRange.value), 'N', '', form.txtHighRange.value, 'ratio');
				break;
			}
		}
	}
	
	Ext.getCmp('queryPanel').show()
}