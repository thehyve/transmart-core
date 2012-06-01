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

function submitBoxPlotJob(form){
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
	
	//This is the selection from the binning dropdown that tells us what we are binning.
	var binningVariable = Ext.get("selBinVariableSelection").getValue()
	
	//This is the final boolean that tells us the validation is okay.
	var finalValidation = false;
	
	//If binning is enabled and we try to bin a categorical value as a continuous, throw an error.
	if(GLOBAL.Binning && binningVariable=="DEP" && Ext.get('variableType').getValue() == 'Continuous' && ((dependentVariableConceptCode != "" && (!dependentNodeList[0] || dependentNodeList[0] == "null")) || (dependentNodeList[0] == 'hleaficon' && window['divDependentVariableSNPType'] == "Genotype" && window['divDependentVariablemarkerType'] == 'SNP')) )
	{
		Ext.Msg.alert('Wrong input', 'There is a categorical input in the Dependent variable box, but you are trying to bin it as if it was continuous. Please alter your binning options or the concept in the Dependent variable box.');
		return;		
	}
	if(GLOBAL.Binning && binningVariable=="IND" && Ext.get('variableType').getValue() == 'Continuous' && ((independentVariableConceptCode != "" && (!independentNodeList[0] || independentNodeList[0] == "null")) || (independentNodeList[0] == 'hleaficon' && window['divIndependentVariableSNPType'] == "Genotype" && window['divIndependentVariablemarkerType'] == 'SNP')) )
	{
		Ext.Msg.alert('Wrong input', 'There is a categorical input in the Independent variable box, but you are trying to bin it as if it was continuous. Please alter your binning options or the concept in the Independent variable box.');
		return;		
	}	

	//If binning is enabled, we are doing categorical and the manual binning checkbox is not checked, alert the user.
	if(GLOBAL.Binning && Ext.get('variableType').getValue() != 'Continuous' && !GLOBAL.ManualBinning)
	{
		Ext.Msg.alert('Wrong input', 'You must enable manual binning when binning a categorical variable.');
		return;			
	}
	
	//If binning is enabled and the user is trying to categorically bin a continuous variable, alert them.
	if(GLOBAL.Binning && binningVariable=="DEP" && Ext.get('variableType').getValue() != 'Continuous' && (dependentNodeList[0] == 'valueicon' || (dependentNodeList[0] == 'hleaficon' && !(window['divDependentVariableSNPType'] == "Genotype" && window['divDependentVariablemarkerType'] == 'SNP'))))
	{
		Ext.Msg.alert('Wrong input', 'You cannot use categorical binning with a continuous variable. Please alter your binning options or the concept in the Dependent box.');
		return;			
	}	
	
	if(GLOBAL.Binning && binningVariable=="IND" && Ext.get('variableType').getValue() != 'Continuous' && (independentNodeList[0] == 'valueicon' || (independentNodeList[0] == 'hleaficon' && !(window['divIndependentVariableSNPType'] == "Genotype" && window['divIndependentVariablemarkerType'] == 'SNP'))))
	{
		Ext.Msg.alert('Wrong input', 'You cannot use categorical binning with a continuous variable. Please alter your binning options or the concept in the Independent box.');
		return;			
	}		
	
	//Nodes will be either 'hleaficon' or 'valueicon'.
	//Box plots require 1 categorical and 1 continuous variable.
	var depVariableType = "";
	var indVariableType = "";
	
	//If there is a categorical variable in either box (This means either of the lists are empty)
	if(!dependentNodeList[0] || dependentNodeList[0] == "null") depVariableType = "CAT";
	if(!independentNodeList[0] || independentNodeList[0] == "null") indVariableType = "CAT";
	
	//If binning is enabled on the dependent variable, then we have a categorical variable.
	if (GLOBAL.Binning && (binningVariable=="DEP")) depVariableType = "CAT";
	if (GLOBAL.Binning && (binningVariable=="IND")) indVariableType = "CAT";
	
	//Dependent: If we have a continuous value or a High Dim Data node (That isn't genotype) and we aren't binning the dependent box, we have a continuous variable. 
	if((dependentNodeList[0] == 'valueicon' || (dependentNodeList[0] == 'hleaficon' && !(window['divDependentVariableSNPType'] == "Genotype" && window['divDependentVariablemarkerType'] == 'SNP'))) && !(GLOBAL.Binning && (binningVariable=="DEP"))) depVariableType = "CON";
	if((independentNodeList[0] == 'valueicon' || (independentNodeList[0] == 'hleaficon' && !(window['divIndependentVariableSNPType'] == "Genotype" && window['divIndependentVariablemarkerType'] == 'SNP'))) && !(GLOBAL.Binning && (binningVariable=="IND"))) indVariableType = "CON";
	
	//If we are doing genotype, mark it as categorical.
	if(dependentNodeList[0] == 'hleaficon' && window['divDependentVariableSNPType'] == "Genotype" && window['divDependentVariablemarkerType'] == 'SNP') depVariableType = "CAT"
	if(independentNodeList[0] == 'hleaficon' && window['divIndependentVariableSNPType'] == "Genotype" && window['divIndependentVariablemarkerType'] == 'SNP') indVariableType = "CAT"
	
	//If we don't have a CON and CAT variable, throw an error.
	if((depVariableType=="CAT" && indVariableType == "CON") || (depVariableType=="CON" && indVariableType == "CAT"))
	{
		finalValidation	= true;
	}
	
	if(!finalValidation)
	{
		Ext.Msg.alert('Wrong input', 'Please select one continuous variable and one categorical variable, with at least 2 data values.');
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
	
	//If the categorical item is in the Dependent variable box, we set the flipping flag.
	var flipImage = false;
	
	if(depVariableType=="CAT")
	{
		flipImage = true;
	}
	
	
	var formParams = {
						dependentVariable:dependentVariableConceptCode,
						independentVariable:independentVariableConceptCode
					};
	
	if(!loadHighDimensionalParameters(formParams)) return false;
	loadBinningParameters(formParams);
	
	//------------------------------------
	//More Validation
	//------------------------------------	
	//If the user dragged in a high dim node, but didn't enter the High Dim Screen, throw an error.
	if(dependentNodeList[0] == 'hleaficon' && formParams["divDependentVariableType"] == "CLINICAL")
	{
		Ext.Msg.alert('Wrong input', 'You dragged a High Dimensional Data node into the dependent variable box but did not select any filters! Please click the "High Dimensional Data" button and select filters. Apply the filters by clicking "Apply Selections".');
		return;			
	}
	if(independentNodeList[0] == 'hleaficon' && formParams["divIndependentVariableType"] == "CLINICAL")
	{
		Ext.Msg.alert('Wrong input', 'You dragged a High Dimensional Data node into the independent variable box but did not select any filters! Please click the "High Dimensional Data" button and select filters. Apply the filters by clicking "Apply Selections".');
		return;			
	}	
	//------------------------------------	
	
	//Pass in our flag that tells us whether to flip or not.
	formParams["flipImage"] = (flipImage) ? 'TRUE' : 'FALSE'
	
	submitJob(formParams);
}

function loadBoxPlotView(){
	registerBoxPlotDragAndDrop();
	clearGroupBox('divIndependentVariable');
	clearGroupBox('divDependentVariable');
	clearHighDimensionalFields();
}

function registerBoxPlotDragAndDrop()
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

function clearGroupBox(divName)
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

function toggleBinning() {
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

function updateManualBinning() {
	// Change the ManualBinning flag.
	GLOBAL.ManualBinning = document.getElementById('chkManualBin').checked;

	// Get the type of the variable we are dealing with.
	variableType = Ext.get('variableType').getValue();
	
	// Hide both DIVs.
	var divContinuous = Ext.get('divManualBinContinuous');
	var divCategorical = Ext.get('divManualBinCategorical');
	divContinuous.setVisibilityMode(Ext.Element.DISPLAY);
	divCategorical.setVisibilityMode(Ext.Element.DISPLAY);
	divContinuous.hide();
	divCategorical.hide();
	
	// Show the div with the binning options relevant to our variable type.
	if (document.getElementById('chkManualBin').checked) {
		if (variableType == "Continuous") {
			divContinuous.show();
			divCategorical.hide();
		} else {
			
			// Find out which variable we are binning.
			var binningVariable = Ext.get("selBinVariableSelection").getValue()
			
			//This will be the box we pull binning choices from.
			var binningSource = ""
			
			//Depending on the variable we are binning, we fill the categorical items box. Handle that logic here.
			if(binningVariable=="DEP")
			{
				binningSource = "divDependentVariable"
			}
			else
			{
				binningSource = "divIndependentVariable"
			}
			
			divContinuous.hide();
			divCategorical.show();
				
			setupCategoricalItemsList(binningSource,"divCategoricalItems");
		}
	}
}



/**
 * When we change the number of bins in the "Number of Bins" input, we have to
 * change the number of bins on the screen.
 */
function manageBins(newNumberOfBins) {

	// This is the row template for a continousBinningRow.
	var tpl = new Ext.Template(
			'<tr id="binningContinousRow{0}">',
			'<td>Bin {0}</td><td><input type="text" id="txtBin{0}RangeLow" /> - <input type="text" id="txtBin{0}RangeHigh" /></td>',
			'</tr>');
	var tplcat = new Ext.Template(
			'<tr id="binningCategoricalRow{0}">',
			'<td><b>Bin {0}</b><div id="divCategoricalBin{0}" class="queryGroupIncludeSmall"></div></td>',
			'</tr>');

	// This is the table we add continuous variables to.
	continuousBinningTable = Ext.get('tblBinContinuous');
	categoricalBinningTable = Ext.get('tblBinCategorical');
	// Clear all old rows out of the table.

	// For each bin, we add a row to the binning table.
	for (i = 1; i <= newNumberOfBins; i++) {
		// If the object isn't already on the screen, add it.
		if (!(Ext.get("binningContinousRow" + i))) {
			tpl.append(continuousBinningTable, [ i ]);
		} else {
			Ext.get("binningContinousRow" + i).show()
		}

		// If the object isn't already on the screen, add it-Categorical
		if (!(Ext.get("binningCategoricalRow" + i))) {
			tplcat.append(categoricalBinningTable, [ i ]);
			// Add the drop targets and handler function.
			var bin = Ext.get("divCategoricalBin" + i);
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
			Ext.get("binningCategoricalRow" + i).show()
		}
	}

	// If the new number of bins is less than the old, hide the old bins.
	if (newNumberOfBins < GLOBAL.NumberOfBins) {
		// For each bin, we add a row to the binning table.
		for (i = parseInt(newNumberOfBins) + 1; i <= GLOBAL.NumberOfBins; i++) {
			// If the object isn't already on the screen, add it.
			if (Ext.get("binningContinousRow" + i)) {
				Ext.get("binningContinousRow" + i).hide();
			}
			// If the object isn't already on the screen, add it.
			if (Ext.get("binningCategoricalRow" + i)) {
				Ext.get("binningCategoricalRow" + i).hide();
			}
		}
	}

	// Set the global variable to reflect the new bin count.
	GLOBAL.NumberOfBins = newNumberOfBins;
	updateManualBinning();
}


function dropOntoBin(source, e, data) {
	this.el.appendChild(data.ddel);
	// Ext.dd.Registry.register(data.ddel, {el : data.ddel});
	return true;
}

function loadBinningParameters(formParams)
{
	
	//These default to FALSE
	formParams["binning"] = "FALSE";
	formParams["manualBinning"] = "FALSE";
	
	// Gather the data from the optional binning items, if we had selected to
	// enable binning.
	if (GLOBAL.Binning) {
		// Get the number of bins the user entered.
		var numberOfBins = Ext.get("txtNumberOfBins").getValue()

		// Get the value from the dropdown that specifies the type of
		// binning.
		var binningType = Ext.get("selBinDistribution").getValue()

		//Get the value from the dropdown that tells us which variable to bin.
		var binningVariable = Ext.get("selBinVariableSelection").getValue()
		
		// Add these items to our form parameters.
		formParams["binning"] = "TRUE";
		formParams["numberOfBins"] = numberOfBins;
		formParams["binDistribution"] = binningType;
		formParams["binVariable"] = binningVariable;

		// If we are using Manual Binning we need to add the parameters
		// here.
		if (GLOBAL.ManualBinning) {

			// Get a bar separated list of bins and their ranges.
			var binRanges = ""

			// Loop over each row in the HTML table.
			var variableType = Ext.get('variableType').getValue();
			if (variableType == "Continuous") {
				for (i = 1; i <= GLOBAL.NumberOfBins; i++) {
					binRanges += "bin" + i + ","
					binRanges += Ext.get('txtBin' + i + 'RangeLow').getValue()
							+ ","
					binRanges += Ext.get('txtBin' + i + 'RangeHigh').getValue()
							+ "|"
				}
			} else {
				for (i = 1; i <= GLOBAL.NumberOfBins; i++) {
					binRanges += "bin" + i + "<>"
					var bin = Ext.get('divCategoricalBin' + i);
					for (x = 0; x < bin.dom.childNodes.length; x++) {
						binRanges+=bin.dom.childNodes[x].getAttribute('conceptdimcode') + "<>"
					}
					binRanges=binRanges.substring(0, binRanges.length - 2);
					binRanges=binRanges+"|";
				}
			}
			formParams["manualBinning"] = "TRUE";
			formParams["binRanges"] = binRanges.substring(0,binRanges.length - 1);
			formParams["variableType"] = Ext.get('variableType').getValue();
		}
	}

}


