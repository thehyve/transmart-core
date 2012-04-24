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

	var variablesConceptCode = dependentVariableConceptCode+"|"+independentVariableConceptCode;
	
	/*
	//Make sure the user entered some items into the variable selection boxes.
	if(dependentVariableConceptCode == '' || (dependentVariableEle.dom.childNodes.length != 1))
		{
			Ext.Msg.alert('Missing input!', 'Please drag one and only one concept into the dependent variable box.');
			return;
		}
	
	if(independentVariableConceptCode == '' || (independentVariableEle.dom.childNodes.length != 1))
		{
			Ext.Msg.alert('Missing input!', 'Please drag one and only one concept into the independent variable box.');
			return;
		}	
	*/
	
	var mrnaData = false
	var snpData = false
	
	var fullGEXGeneList = "";
	var fullSNPGeneList = "";
	var fullSNPGPL 		= "";
	var fullGEXGPL 		= "";
	
	var dependentGeneList 	= window['divDependentVariablepathway'];
	var dependentPlatform 	= window['divDependentVariableplatforms1'];
	var dependentType 		= window['divDependentVariablemarkerType'];
	var dependentGPL		= window['divDependentVariablegplValues'];
	
	var independentGeneList = window['divIndependentVariablepathway'];	
	var independentType		= window['divIndependentVariablemarkerType'];
	var independentPlatform = window['divIndependentVariableplatforms1'];
	var independentGPL		= window['divIndependentVariablegplValues'];
	
	//This variable holds all the GPLs for the two subsets for each input box. We only ever have one subset per input box in the scatter plot currently. Take only the 0 indexed GPL ID.
	if(dependentGPL) dependentGPL = dependentGPL[0];
	if(independentGPL) independentGPL = independentGPL[0];
	
	var logX = form.logX.checked;

	//If we are using High Dimensional data we need to create variables that represent genes from both independent and dependent selections (In the event they are both of a single high dimensional type).
	//Check to see if the user selected GEX in the independent input.
	if(independentType == "Gene Expression")
	{
		//The genes entered into the search box were GEX genes.
		fullGEXGeneList = independentGeneList;
		fullGEXGPL = independentGPL;
			
		//This flag will tell us to write the GEX text file.
		mrnaData = true;
		
		//Fix the platform to be something the R script expects.
		independentType = "MRNA";
	}
	
	if(dependentType == "Gene Expression")
	{
		//If the gene list already has items, add a comma.
		if(fullGEXGeneList != "") 	fullGEXGeneList += ","
		if(fullGEXGPL != "") 		fullGEXGPL += ","
				
		//Add the genes in the list to the full list of GEX genes.
		fullGEXGeneList += dependentGeneList
		fullGEXGPL += dependentGPL;
		
		//This flag will tell us to write the GEX text file.		
		mrnaData = true;
		
		//Fix the platform to be something the R script expects.
		dependentType = "MRNA";
	}
	
	//Check to see if the user selected SNP in the independent input.
	if(independentType == "SNP")
	{
		//The genes entered into the search box were SNP genes.
		fullSNPGeneList = independentGeneList;
		fullSNPGPL = independentGPL;
		
		//This flag will tell us to write the SNP text file.
		snpData = true;
	}
	
	if(dependentType == "SNP")
	{
		//If the gene list already has items, add a comma.
		if(fullSNPGeneList != "") fullGEXGeneList += ","
		if(fullSNPGPL != "") fullSNPGPL += ","
		
		//Add the genes in the list to the full list of SNP genes.
		fullSNPGeneList += dependentGeneList
		fullSNPGPL += dependentGPL;
		
		//This flag will tell us to write the SNP text file.		
		snpData = true;
	}	
	
	if((fullGEXGeneList == "") && (independentType == "MRNA" || dependentType == "MRNA"))
	{
		Ext.Msg.alert("No Genes Selected!", "Please specify Genes in the Gene/Pathway Search box.")
		return false;
	}
	
	if((fullSNPGeneList == "") && (independentType == "SNP" || dependentType == "SNP"))
	{
		Ext.Msg.alert("No Genes Selected!", "Please specify Genes in the Gene/Pathway Search box.")
		return false;
	}	
		
	//If we don't have a platform, fill in Clinical.
	if(dependentPlatform == null || dependentPlatform == "") dependentType = "CLINICAL"
	if(independentPlatform == null || independentPlatform == "") independentType = "CLINICAL"
	
	var formParams = {
			dependentVariable:						dependentVariableConceptCode,
			independentVariable:					independentVariableConceptCode,
			divDependentVariabletimepoints:			window['divDependentVariabletimepoints1'],
			divDependentVariablesamples:			window['divDependentVariablesamples1'],
			divDependentVariablerbmPanels:			window['divDependentVariablerbmPanels1'],
			divDependentVariableplatforms:			dependentPlatform,
			divDependentVariablegpls:				window['divDependentVariablegplsValue1'],
			divDependentVariabletissues:			window['divDependentVariabletissues1'],
			divDependentVariableprobesAggregation:	window['divDependentVariableprobesAggregation'],
			divDependentVariableSNPType:			window['divDependentVariableSNPType'],
			divDependentVariableType:				dependentType,
			divDependentVariablePathway:			dependentGeneList,
			divIndependentVariabletimepoints:		window['divIndependentVariabletimepoints1'],
			divIndependentVariablesamples:			window['divIndependentVariablesamples1'],
			divIndependentVariablerbmPanels:		window['divIndependentVariablerbmPanels1'],
			divIndependentVariableplatforms:		independentPlatform,
			divIndependentVariablegpls:				window['divIndependentVariablegplsValue1'],
			divIndependentVariabletissues:			window['divIndependentVariabletissues1'],
			divIndependentVariableprobesAggregation:window['divIndependentVariableprobesAggregation'],
			divIndependentVariableSNPType:			window['divIndependentVariableSNPType'],
			divIndependentVariableType:				independentType,
			divIndependentVariablePathway:			independentGeneList,
			gexpathway:								fullGEXGeneList,
			snppathway:								fullSNPGeneList,
			divIndependentPathwayName:				window['divIndependentVariablepathwayName'],
			divDependentPathwayName:				window['divDependentVariablepathwayName'],
			mrnaData:								mrnaData,
			snpData:								snpData,
			variablesConceptPaths:					variablesConceptCode,
			logX:									logX,
			gexgpl:									fullGEXGPL,
			snpgpl:									fullSNPGPL,
			jobType:								'ScatterPlot'			
	};
	
	submitJob(formParams);
}

/**
 * Register drag and drop.
 * Clear out all gobal variables and reset them to blank.
 */
function loadScatterPlotView(){
	registerScatterPlotDragAndDrop();
	clearHighDimDataSelections('divIndependentVariable');
	clearHighDimDataSelections('divDependentVariable');
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