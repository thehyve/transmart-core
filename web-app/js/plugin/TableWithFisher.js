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

function submitTableWithFisherJob(form){
	var dependentVariableEle = Ext.get("divDependentVariable");
	var independentVariableEle = Ext.get("divIndependentVariable");
	
	var dependentVariableConceptCode = "";
	var independentVariableConceptCode = "";	
	
	//If the category variable element has children, we need to parse them and concatenate their values.
	if(independentVariableEle.dom.childNodes[0])
	{
		//Loop through the category variables and add them to a comma seperated list.
		for(nodeIndex = 0; nodeIndex < independentVariableEle.dom.childNodes.length; nodeIndex++)
		{
			//If we already have a value, add the seperator.
			if(independentVariableConceptCode != '') independentVariableConceptCode += '|' 
			
			//Add the concept path to the string.
				independentVariableConceptCode += getQuerySummaryItem(independentVariableEle.dom.childNodes[nodeIndex]).trim()
		}
	}	
	
	//If the category variable element has children, we need to parse them and concatenate their values.
	if(dependentVariableEle.dom.childNodes[0])
	{
		//Loop through the category variables and add them to a comma seperated list.
		for(nodeIndex = 0; nodeIndex < dependentVariableEle.dom.childNodes.length; nodeIndex++)
		{
			//If we already have a value, add the seperator.
			if(dependentVariableConceptCode != '') dependentVariableConceptCode += '|' 
			
			//Add the concept path to the string.
			dependentVariableConceptCode += getQuerySummaryItem(dependentVariableEle.dom.childNodes[nodeIndex]).trim()
		}
	}	
	
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

	//Fisher test requires two categorical variables.
	var depVariableType = "";
	var indVariableType = "";	
	
	//Loop through the dependent variable box and find the the of nodes in the box.
	var dependentNodeList = createNodeTypeArrayFromDiv(dependentVariableEle,"setnodetype")
	var independentNodeList = createNodeTypeArrayFromDiv(independentVariableEle,"setnodetype")
	
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
	
	//If binning is enabled, we are doing categorical and the manual binning checkbox is not checked, alert the user.
	if(GLOBAL.Binning && Ext.get('variableTypeIndep').getValue() != 'Continuous' && !document.getElementById("chkManualBinIndep").checked && document.getElementById('EnableBinningIndep').checked)
	{
		Ext.Msg.alert('Wrong input', 'You must enable manual binning when binning a categorical variable. (Independent Variable)');
		return;			
	}	
	if(GLOBAL.Binning && Ext.get('variableTypeDep').getValue() != 'Continuous' && !document.getElementById("chkManualBinDep").checked && document.getElementById('EnableBinningDep').checked)
	{
		Ext.Msg.alert('Wrong input', 'You must enable manual binning when binning a categorical variable. (Dependent Variable)');
		return;			
	}	
	
	//If binning is enabled and we try to bin a categorical value as a continuous, throw an error.
	if(GLOBAL.Binning && document.getElementById('EnableBinningDep').checked && Ext.get('variableTypeDep').getValue() == 'Continuous' && ((dependentVariableConceptCode != "" && (!dependentNodeList[0] || dependentNodeList[0] == "null")) || (dependentNodeList[0] == 'hleaficon' && window['divDependentVariableSNPType'] == "Genotype" && window['divDependentVariablemarkerType'] == 'SNP')) )
	{
		Ext.Msg.alert('Wrong input', 'There is a categorical input in the Dependent variable box, but you are trying to bin it as if it was continuous. Please alter your binning options or the concept in the Dependent variable box.');
		return;		
	}	
	if(GLOBAL.Binning && document.getElementById('EnableBinningIndep').checked && Ext.get('variableTypeIndep').getValue() == 'Continuous' && ((independentVariableConceptCode != "" && (!independentNodeList[0] || independentNodeList[0] == "null")) || (independentNodeList[0] == 'hleaficon' && window['divIndependentVariableSNPType'] == "Genotype" && window['divIndependentVariablemarkerType'] == 'SNP')) )
	{
		Ext.Msg.alert('Wrong input', 'There is a categorical input in the Independent variable box, but you are trying to bin it as if it was continuous. Please alter your binning options or the concept in the Independent variable box.');
		return;		
	}	
	
	//These tell us if we are binning the dependent or independent box.
	if(GLOBAL.Binning && document.getElementById('EnableBinningIndep').checked) indVariableType = "CAT"
	if(GLOBAL.Binning && document.getElementById('EnableBinningDep').checked) depVariableType = "CAT"
	
	//If there is a categorical variable in either box (This means either of the lists are empty)
	if(!dependentNodeList[0] || dependentNodeList[0] == "null") depVariableType = "CAT";
	if(!independentNodeList[0] || independentNodeList[0] == "null") indVariableType = "CAT";	

	//The last type of category is if we have high dim data, using SNP(Genotype).
	if(dependentNodeList[0] == 'hleaficon' && window['divDependentVariableSNPType'] == "Genotype" && window['divDependentVariablemarkerType'] == 'SNP') depVariableType = "CAT";
	if(independentNodeList[0] == 'hleaficon' && window['divIndependentVariableSNPType'] == "Genotype" && window['divIndependentVariablemarkerType'] == 'SNP') indVariableType = "CAT";
	
	//Check to make sure we have two categorical values.
	if(!(depVariableType=="CAT"))
	{
		Ext.Msg.alert('Wrong input', 'Fisher Table requires 2 categorical variables and the dependent variable is not categorical.');
		return;		
	}
	
	if(!(indVariableType=="CAT"))
	{
		Ext.Msg.alert('Wrong input', 'Fisher Table requires 2 categorical variables and the independent variable is not categorical.');
		return;		
	}

	//If the dependent node list is empty but we have a concept in the box (Meaning we dragged in categorical items) and there is only one item in the box, alert the user. 
	if((!dependentNodeList[0] || dependentNodeList[0] == "null") && dependentVariableConceptCode.indexOf("|") == -1)
	{
		Ext.Msg.alert('Wrong input', 'When using categorical variables you must use at least 2. The dependent box only has 1 categorical variable in it.');
		return;			
	}
	
	if((!independentNodeList[0] || independentNodeList[0] == "null") && independentVariableConceptCode.indexOf("|") == -1)
	{
		Ext.Msg.alert('Wrong input', 'When using categorical variables you must use at least 2. The independent box only has 1 categorical variable in it.');
		return;			
	}	
	//------------------------------------
	
	variablesConceptCode = dependentVariableConceptCode + "|" + independentVariableConceptCode;
	
	var formParams = {dependentVariable:dependentVariableConceptCode,
			independentVariable:independentVariableConceptCode,
			jobType:'TableWithFisher',
			variablesConceptPaths:variablesConceptCode};
	
	if(!loadHighDimensionalParameters(formParams)) return false;
	loadBinningParametersFisher(formParams);
	
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

function loadTableWithFisherView(){
	registerFisherDragAndDrop();
	clearGroupFisher('divIndependentVariable');
	clearGroupFisher('divDependentVariable');
	clearHighDimensionalFields();
}

function clearGroupFisher(divName) {
	// Clear the drag and drop div.
	var qc = Ext.get(divName);

	for ( var i = qc.dom.childNodes.length - 1; i >= 0; i--) {
		var child = qc.dom.childNodes[i];
		qc.dom.removeChild(child);
	}

	clearHighDimDataSelections(divName);
	clearSummaryDisplay(divName);
}

function toggleBinningFisher() {
	// Change the Binning flag.
	GLOBAL.Binning = !GLOBAL.Binning;

	// Change the toggle button text.
	if (GLOBAL.Binning) 
	{
		//Toggle the div with the binning options.		
		document.getElementById('divBinning').style.display = '';
		
		document.getElementById('BinningToggle').value = "Disable"
		
	} else 
	{
		//Toggle the div with the binning options.		
		document.getElementById('divBinning').style.display='none';
		
		document.getElementById('BinningToggle').value = "Enable"
	}
}

function updateManualBinningFisher(binningSuffix) {
	// Change the ManualBinning flag.
	GLOBAL.ManualBinning = document.getElementById('chkManualBinDep').checked || document.getElementById('chkManualBinIndep').checked;

	// Get the type of the variable we are dealing with.
	variableType = Ext.get('variableType' + binningSuffix).getValue();

	// Hide both DIVs.
	var divContinuous = Ext.get('divManualBinContinuous' + binningSuffix);
	var divCategorical = Ext.get('divManualBinCategorical' + binningSuffix);
	divContinuous.setVisibilityMode(Ext.Element.DISPLAY);
	divCategorical.setVisibilityMode(Ext.Element.DISPLAY);
	divContinuous.hide();
	divCategorical.hide();
	
	// Show the div with the binning options relevant to our variable type.
	if (document.getElementById('chkManualBin' + binningSuffix).checked) {
		if (variableType == "Continuous") {
			divContinuous.show();
			divCategorical.hide();
		} else {
			divContinuous.hide();
			divCategorical.show();
			
			//We need to make sure we choose the values from the proper category.
			if(binningSuffix=="Dep")
			{
				setupCategoricalItemsList("divDependentVariable","divCategoricalItems" + binningSuffix);
			}
			else if(binningSuffix=="Indep")
			{
				setupCategoricalItemsList("divIndependentVariable","divCategoricalItems" + binningSuffix);
			}
			
		}
	}
}

/**
 * When we change the number of bins in the "Number of Bins" input, we have to
 * change the number of bins on the screen.
 */
function manageBinsFisher(newNumberOfBins,binningSuffix) {

	// This is the row template for a continousBinningRow.
	var tpl = new Ext.Template(
			'<tr id="binningContinousRow{0}{1}">',
			'<td>Bin {0}</td><td><input type="text" id="txtBin{0}{1}RangeLow" /> - <input type="text" id="txtBin{0}{1}RangeHigh" /></td>',
			'</tr>');
	var tplcat = new Ext.Template(
			'<tr id="binningCategoricalRow{0}{1}">',
			'<td><b>Bin {0}</b><div id="divCategoricalBin{0}{1}" class="queryGroupIncludeSmall"></div></td>',
			'</tr>');

	// This is the table we add continuous variables to.
	continuousBinningTable = Ext.get('tblBinContinuous' + binningSuffix);
	categoricalBinningTable = Ext.get('tblBinCategorical' + binningSuffix);

	// For each bin, we add a row to the binning table.
	for (i = 1; i <= newNumberOfBins; i++) {
		// If the object isn't already on the screen, add it.
		if (!(Ext.get("binningContinousRow" + i  + binningSuffix))) {
			tpl.append(continuousBinningTable, [ i, binningSuffix ]);
		} else {
			Ext.get("binningContinousRow" + i  + binningSuffix).show()
		}

		// If the object isn't already on the screen, add it-Categorical
		if (!(Ext.get("binningCategoricalRow" + i  + binningSuffix))) {
			tplcat.append(categoricalBinningTable, [ i, binningSuffix]);
			// Add the drop targets and handler function.
			var bin = Ext.get("divCategoricalBin" + i  + binningSuffix);
			var dragZone = new Ext.dd.DragZone(bin, {
				ddGroup : 'makeBin',
				isTarget: true,
				ignoreSelf: false
			
			});
			var dropZone = new Ext.dd.DropTarget(bin, {
				ddGroup : 'makeBin',
				isTarget: true,
				ignoreSelf: false
			});
			// dropZone.notifyEnter = test;
			dropZone.notifyDrop = dropOntoBin; // dont forget to make each
			// dropped
			// node a drag target
		} else {
			Ext.get("binningCategoricalRow" + i  + binningSuffix).show()
		}
	}

	// If the new number of bins is less than the old, hide the old bins.
	if (newNumberOfBins < GLOBAL.NumberOfBins) {
		// For each bin, we add a row to the binning table.
		for (i = parseInt(newNumberOfBins) + 1; i <= GLOBAL.NumberOfBins; i++) {
			// If the object isn't already on the screen, add it.
			if (Ext.get("binningContinousRow" + i  + binningSuffix)) {
				Ext.get("binningContinousRow" + i  + binningSuffix).hide();
			}
			// If the object isn't already on the screen, add it.
			if (Ext.get("binningCategoricalRow" + i  + binningSuffix)) {
				Ext.get("binningCategoricalRow" + i  + binningSuffix).hide();
			}
		}
	}

	// Set the global variable to reflect the new bin count.
	GLOBAL.NumberOfBins = newNumberOfBins;
	updateManualBinningFisher(binningSuffix);
}


function registerFisherDragAndDrop()
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

function loadBinningParametersFisher(formParams)
{
	// These default to FALSE
	formParams["binningDep"] = "FALSE";
	formParams["binningIndep"] = "FALSE";
	formParams["manualBinningDep"] = "FALSE";
	formParams["manualBinningIndep"] = "FALSE";
	
	// Gather the data from the optional binning items, if we had selected to enable binning. Only gather if one of the binning sections is checked.
	if (GLOBAL.Binning && (document.getElementById('EnableBinningDep').checked || document.getElementById('EnableBinningIndep').checked)) 
	{
		//This is the list of suffixes we used on the HTML input IDs.
		var suffixList = ['Dep','Indep'];
		
		//Loop through each suffix and run some logic.
		for(j=0;j<suffixList.size();j++)
		{
			//Grab the current suffix.
			var currentSuffix = suffixList[j];
		
			//If we enabled binning for this variable.
			if(document.getElementById('EnableBinning' + currentSuffix).checked)
			{
				//Set the flag that will force this variable to be binned.
				formParams["binning" + currentSuffix] = "TRUE";
				//Grab the number of bins from HTML input.
				formParams["numberOfBins" + currentSuffix] = Ext.get("txtNumberOfBins" + currentSuffix).getValue();
				//Get the bin distribution type that is used in Continuous variables.
				formParams["binDistribution" + currentSuffix] = Ext.get("selBinDistribution" + currentSuffix).getValue();			
			}				
		
		}
		
		//If we are using Manual Binning we need to add the parameters here. We also check to see that at least one of the manual binning checkboxes is checked.
		if (GLOBAL.ManualBinning && (document.getElementById('chkManualBinDep').checked || document.getElementById('chkManualBinIndep').checked)) 
		{
			//Loop through the list of HTML suffixes.
			for(j=0;j<suffixList.size();j++)
			{
				//Grab the suffix.
				var currentSuffix = suffixList[j];
				
				//Only do this if the checkbox for binning this variable manually is checked.
				if(document.getElementById('chkManualBin' + currentSuffix).checked)
				{
					// Get a bar separated list of bins and their ranges.
					var binRanges = ""
	
					// Loop over each row in the HTML table.
					var variableType = Ext.get('variableType' + currentSuffix).getValue();
					
					//Depending on the variable type, build the binning string differently. 
					if (variableType == "Continuous") 
					{
						for (i = 1; i <= GLOBAL.NumberOfBins; i++) 
						{
							binRanges += "bin" + i + ","
							binRanges += Ext.get('txtBin' + i + currentSuffix + 'RangeLow').getValue() + ","
							binRanges += Ext.get('txtBin' + i + currentSuffix + 'RangeHigh').getValue()	+ "|"
						}
					} 
					else 
					{
						for (i = 1; i <= GLOBAL.NumberOfBins; i++) 
						{
							binRanges += "bin" + i + "<>"
							
							var bin = Ext.get('divCategoricalBin' + i + currentSuffix);
							
							for (x = 0; x < bin.dom.childNodes.length; x++) 
							{
								binRanges+=bin.dom.childNodes[x].getAttribute('conceptdimcode') + "<>"
							}
							
							binRanges=binRanges.substring(0, binRanges.length - 2);
							binRanges=binRanges+"|";
						}
					}
					
					formParams["manualBinning" + currentSuffix] = "TRUE";
					formParams["binRanges" + currentSuffix] = binRanges.substring(0,binRanges.length - 1);
					formParams["variableType" + currentSuffix] = variableType;
				}
			}
		}
	}	

}

