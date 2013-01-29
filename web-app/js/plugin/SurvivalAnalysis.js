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

function submitSurvivalJob(form) {
	var timeVariableEle = Ext.get("divTimeVariable");
	var categoryVariableEle = Ext.get("divCategoryVariable");
	var censoringVariableEle = Ext.get("divCensoringVariable");

	var timeVariableConceptCode = "";
	var categoryVariableConceptCode = "";
	var censoringVariableConceptCode = "";

	if (timeVariableEle.dom.childNodes[0])
		timeVariableConceptCode = getQuerySummaryItem(timeVariableEle.dom.childNodes[0]);

	// If the category variable element has children, we need to parse them and
	// concatenate their values.
	if (categoryVariableEle.dom.childNodes[0]) {
		// Loop through the category variables and add them to a comma seperated
		// list.
		for (nodeIndex = 0; nodeIndex < categoryVariableEle.dom.childNodes.length; nodeIndex++) {
			// If we already have a value, add the seperator.
			if (categoryVariableConceptCode != '')
				categoryVariableConceptCode += '|'

				// Add the concept path to the string.
			categoryVariableConceptCode += getQuerySummaryItem(
					categoryVariableEle.dom.childNodes[nodeIndex]).trim()
		}
	}

	if (censoringVariableEle.dom.childNodes[0])
		censoringVariableConceptCode = getQuerySummaryItem(censoringVariableEle.dom.childNodes[0]);

	
	//------------------------------------
	//Validation
	//------------------------------------
	
	//Validate the input box for time.
	if(timeVariableConceptCode == '')
	{
		Ext.Msg.alert('Missing input', 'Please drag at least one concept into the time variable box.');
		return;
	}
		
	//This will tell us the type of nodes drag into each box.
	var timeNodeList = createNodeTypeArrayFromDiv(timeVariableEle,"setnodetype")
	var categoryNodeList = createNodeTypeArrayFromDiv(categoryVariableEle,"setnodetype")
	var censoringNodeList = createNodeTypeArrayFromDiv(censoringVariableEle,"setnodetype")
	
	//If the user dragged in multiple node types, throw an error.
	if(timeNodeList.length > 1)
	{
		Ext.Msg.alert('Wrong input', 'You may only drag nodes of the same type (Continuous,Categorical,High Dimensional) into the input box. The Time input box has multiple types.');
		return;		
	}			

	if(categoryNodeList.length > 1)
	{
		Ext.Msg.alert('Wrong input', 'You may only drag nodes of the same type (Continuous,Categorical,High Dimensional) into the input box. The Category input box has multiple types.');
		return;		
	}			

	if(censoringNodeList.length > 1)
	{
		Ext.Msg.alert('Wrong input', 'You may only drag nodes of the same type (Continuous,Categorical,High Dimensional) into the input box. The Censoring input box has multiple types.');
		return;		
	}			
	
	//For the valueicon and hleaficon nodes, you can only put one in a given input box.
	if((timeNodeList[0] == 'valueicon' || timeNodeList[0] == 'hleaficon') && (timeVariableConceptCode.indexOf("|") != -1))
	{
		Ext.Msg.alert('Wrong input', 'For continuous and high dimensional data, you may only drag one node into the input boxes. The Time input box has multiple nodes.');
		return;		
	}		

	if((categoryNodeList[0] == 'valueicon' || categoryNodeList[0] == 'hleaficon') && (categoryVariableConceptCode.indexOf("|") != -1))
	{
		Ext.Msg.alert('Wrong input', 'For continuous and high dimensional data, you may only drag one node into the input boxes. The Category input box has multiple nodes.');
		return;		
	}		

	if((censoringNodeList[0] == 'valueicon' || censoringNodeList[0] == 'hleaficon') && (censoringVariableConceptCode.indexOf("|") != -1))
	{
		Ext.Msg.alert('Wrong input', 'For continuous and high dimensional data, you may only drag one node into the input boxes. The Censoring input box has multiple nodes.');
		return;		
	}		
	
	//If binning is enabled and we try to bin a categorical value as a continuous, throw an error.
	if(GLOBAL.Binning && Ext.get('variableType').getValue() == 'Continuous' && ((categoryVariableConceptCode != "" && (!categoryNodeList[0] || categoryNodeList[0] == "null")) || (categoryNodeList[0] == 'hleaficon' && window['divCategoryVariableSNPType'] == "Genotype" && window['divCategoryVariablemarkerType'] == 'SNP')) )
	{
		Ext.Msg.alert('Wrong input', 'There is a categorical input in the Category box, but you are trying to bin it as if it was continuous. Please alter your binning options or the concept in the Category box.');
		return;		
	}	
	
	//If binning is enabled, we are doing categorical and the manual binning checkbox is not checked, alert the user.
	if(GLOBAL.Binning && Ext.get('variableType').getValue() != 'Continuous' && !GLOBAL.ManualBinning)
	{
		Ext.Msg.alert('Wrong input', 'You must enable manual binning when binning a categorical variable.');
		return;			
	}
	
	//Nodes will be either 'hleaficon' or 'valueicon'.
	//Survival requires the time be numeric, category be categorical, and censor be categorical. Category and Censor are not required.
	var categoryVariableType = "";
	
	//If Category is not empty, check to see if the node value is categorical.
	if(categoryVariableConceptCode != "" && (!categoryNodeList[0] || categoryNodeList[0] == "null")) categoryVariableType = "CAT";
	
	//We only bin on category here, so if binning is enabled the category is categorical.
	if (GLOBAL.Binning) categoryVariableType = "CAT";
	
	//Check to see if the category node is continuous.
	if((categoryNodeList[0] == 'valueicon' || (categoryNodeList[0] == 'hleaficon' && !(window['divCategoryVariableSNPType'] == "Genotype" && window['divCategoryVariablemarkerType'] == 'SNP'))) && !(GLOBAL.Binning)) categoryVariableType = "CON";
	
	//If binning is enabled and the user is trying to categorically bin a continuous variable, alert them.
	if(GLOBAL.Binning && Ext.get('variableType').getValue() != 'Continuous' && (categoryNodeList[0] == 'valueicon' || (categoryNodeList[0] == 'hleaficon' && !(window['divCategoryVariableSNPType'] == "Genotype" && window['divCategoryVariablemarkerType'] == 'SNP'))))
	{
		Ext.Msg.alert('Wrong input', 'You cannot use categorical binning with a continuous variable. Please alter your binning options or the concept in the Category box.');
		return;			
	}		
	
	//If time is not a '123' node, throw an error.
	if(timeNodeList[0] != 'valueicon')
	{
		Ext.Msg.alert('Wrong input', 'Survival Analysis requires a continuous variable that is not high dimensional in the "Time" box.');
		return;
	}
	
	//If there is a node in the censoring box and it isn't a category, throw an error.
	if(censoringVariableConceptCode != "" && !(!censoringNodeList[0] || censoringNodeList[0] == "null")) 
	{
		Ext.Msg.alert('Wrong input', 'Survival Analysis requires categorical variables in the "Censoring Variable" box.');
		return;
	}		
	
	if(categoryVariableConceptCode != "" && categoryVariableType != "CAT")
	{
		Ext.Msg.alert('Wrong input', 'Survival Analysis requires categorical variables in the "Category" box.');
		return;
	}

	//If the dependent node list is empty but we have a concept in the box (Meaning we dragged in categorical items) and there is only one item in the box, alert the user. 
	if(categoryVariableConceptCode != "" && (!categoryNodeList[0] || categoryNodeList[0] == "null") && categoryVariableConceptCode.indexOf("|") == -1)
	{
		Ext.Msg.alert('Wrong input', 'When using categorical variables you must use at least 2. The dependent box only has 1 categorical variable in it.');
		return;			
	}	
	
	//------------------------------------

	//Create a string of all the concepts we need for the i2b2 data.
	variablesConceptCode = timeVariableConceptCode;
	if(categoryVariableConceptCode != "") variablesConceptCode += "|" + categoryVariableConceptCode
	if(censoringVariableConceptCode != "") variablesConceptCode += "|" + censoringVariableConceptCode
	
	var formParams = {
		timeVariable : timeVariableConceptCode,
		categoryVariable : categoryVariableConceptCode,
		censoringVariable : censoringVariableConceptCode,
		variablesConceptPaths:					variablesConceptCode
	};

	if(!loadHighDimensionalParametersSurvival(formParams)) return false;
	loadBinningParametersSurvival(formParams);
	
	//------------------------------------
	//More Validation
	//------------------------------------	
	//If the user dragged in a high dim node, but didn't enter the High Dim Screen, throw an error.
	if(categoryNodeList[0] == 'hleaficon' && formParams["divDependentVariableType"] == "CLINICAL")
	{
		Ext.Msg.alert('Wrong input', 'You dragged a High Dimensional Data node into the category variable box but did not select any filters! Please click the "High Dimensional Data" button and select filters. Apply the filters by clicking "Apply Selections".');
		return;			
	}	
	//------------------------------------
	
	submitJob(formParams);
}

function loadSurvivalAnalysisView() {
	registerSurvivalDragAndDrop();
	clearGroupSurvival('divTimeVariable');
	clearGroupSurvival('divCategoryVariable');
	clearGroupSurvival('divCensoringVariable');
	clearHighDimensionalFields();
}

function registerSurvivalDragAndDrop() {
	// Set up drag and drop for Dependent and Independent variables on the data
	// association tab.
	// Get the Dependent DIV.
	var timeDiv = Ext.get("divTimeVariable");
	// Get the Independent DIV
	var categoryDiv = Ext.get("divCategoryVariable");
	// Get the Censoring DIV
	var censoringDiv = Ext.get("divCensoringVariable");

	// Add the drop targets and handler function.
	dtgD = new Ext.dd.DropTarget(timeDiv, {
		ddGroup : 'makeQuery'
	});
	dtgD.notifyDrop = dropNumericOntoCategorySelection;

	dtgI = new Ext.dd.DropTarget(categoryDiv, {
		ddGroup : 'makeQuery'
	});
	dtgI.notifyDrop = dropOntoCategorySelection;

	dtgI = new Ext.dd.DropTarget(censoringDiv, {
		ddGroup : 'makeQuery'
	});
	dtgI.notifyDrop = dropOntoCategorySelection;

}

function clearGroupSurvival(divName) 
{
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

function updateManualBinningSurvival() {
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
			divContinuous.hide();
			divCategorical.show();
			setupCategoricalItemsList("divCategoryVariable","divCategoricalItems");
		}
	}
}



/**
 * When we change the number of bins in the "Number of Bins" input, we have to
 * change the number of bins on the screen.
 */
function manageBinsSurvival(newNumberOfBins) {

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
	updateManualBinningSurvival();
}


function dropOntoBin(source, e, data) {
	this.el.appendChild(data.ddel);
	// Ext.dd.Registry.register(data.ddel, {el : data.ddel});
	return true;
}

function loadHighDimensionalParametersSurvival(formParams)
{
	//These will tell tranSMART what data types we need to retrieve.
	var mrnaData = false
	var snpData = false

	//Gene expression filters.
	var fullGEXSampleType 	= "";
	var fullGEXTissueType 	= "";
	var fullGEXTime 		= "";
	var fullGEXGeneList 	= "";
	var fullGEXGPL 			= "";
	
	//SNP Filters.
	var fullSNPSampleType 	= "";
	var fullSNPTissueType 	= "";
	var fullSNPTime 		= "";	
	var fullSNPGeneList 	= "";
	var fullSNPGPL 			= "";
	
	var categoryGeneList 	= window['divCategoryVariablepathway'];
	var categoryPlatform 	= window['divCategoryVariableplatforms1'];
	var categoryType		= window['divCategoryVariablemarkerType'];
	var categorySampleType	= window['divCategoryVariablesamplesValues'];
	var categoryTissueType	= window['divCategoryVariabletissuesValues'];
	var categoryTime		= window['divCategoryVariabletimepointsValues'];
	var categoryGPL			= window['divCategoryVariablegplValues'];
	
	//This variable holds all the GPLs for the two subsets for each input box. We only ever have one subset per input box in the scatter plot currently. Take only the 0 indexed GPL ID.
	if(categoryGPL) 		categoryGPL = categoryGPL[0];
	if(categorySampleType) 	categorySampleType = categorySampleType[0];
	if(categoryTissueType) 	categoryTissueType = categoryTissueType[0];
	if(categoryTime) 		categoryTime = categoryTime[0];
	
	//If we are using High Dimensional data we need to create variables that represent genes from both independent and dependent selections (In the event they are both of a single high dimensional type).
	//Check to see if the user selected GEX in the independent input
	if(categoryType == "Gene Expression")
	{
		//The genes entered into the search box were GEX genes.
		fullGEXGeneList 	= String(categoryGeneList);
		fullGEXSampleType 	= String(categorySampleType);
		fullGEXTissueType 	= String(categoryTissueType);
		fullGEXTime			= String(categoryTime)
		fullGEXGPL 			= String(categoryGPL);
		
		//This flag will tell us to write the GEX text file.
		mrnaData = true;
		
		//Fix the platform to be something the R script expects.
		categoryType = "MRNA";		
	}
	
	//Check to see if the user selected SNP in the independent input.
	if(categoryType == "SNP")
	{
		//The genes entered into the search box were SNP genes.
		fullSNPGeneList 	= String(categoryGeneList);
		fullSNPSampleType 	= String(categorySampleType);
		fullSNPTissueType 	= String(categoryTissueType);
		fullSNPTime 		= String(categoryTime);
		fullSNPGPL 			= String(categoryGPL);
		
		//This flag will tell us to write the SNP text file.
		snpData = true;
	}
	
	if((fullGEXGeneList == "") && (categoryType == "MRNA"))
	{
		Ext.Msg.alert("No Genes Selected", "Please specify Genes in the Gene/Pathway Search box.")
		return false;
	}
	
	if((fullSNPGeneList == "") && (categoryType == "SNP"))
	{
		Ext.Msg.alert("No Genes Selected", "Please specify Genes in the Gene/Pathway Search box.")
		return false;
	}
		
	//If we don't have a platform, fill in Clinical.
	if(categoryPlatform == null || categoryPlatform == "") categoryType = "CLINICAL"
	
	formParams["divDependentVariabletimepoints"] 			= window['divCategoryVariabletimepoints1'];
	formParams["divDependentVariablesamples"] 				= window['divCategoryVariablesamples1'];
	formParams["divDependentVariablerbmPanels"]				= window['divCategoryVariablerbmPanels1'];
	formParams["divDependentVariableplatforms"]				= categoryPlatform
	formParams["divDependentVariablegpls"]					= window['divCategoryVariablegplsValue1'];
	formParams["divDependentVariabletissues"]				= window['divCategoryVariabletissues1'];
	formParams["divDependentVariableprobesAggregation"]	 	= window['divCategoryVariableprobesAggregation'];
	formParams["divDependentVariableSNPType"]				= window['divCategoryVariableSNPType'];
	formParams["divDependentVariableType"]					= categoryType;
	formParams["divDependentVariablePathway"]				= categoryGeneList;
	formParams["divDependentPathwayName"]					= window['divCategoryVariablepathwayName'];	
	formParams["gexpathway"]								= fullGEXGeneList;
	formParams["gextime"]									= fullGEXTime;
	formParams["gextissue"]									= fullGEXTissueType;
	formParams["gexsample"]									= fullGEXSampleType;
	formParams["snppathway"]								= fullSNPGeneList;
	formParams["snptime"]									= fullSNPTime;
	formParams["snptissue"]									= fullSNPTissueType;
	formParams["snpsample"]									= fullSNPSampleType;
	formParams["mrnaData"]									= mrnaData;
	formParams["snpData"]									= snpData;
	formParams["gexgpl"]									= fullGEXGPL;
	formParams["snpgpl"]									= fullSNPGPL;
	formParams["jobType"]									= 'SurvivalAnalysis';

	return true;
	
}

function loadBinningParametersSurvival(formParams)
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
		//var binningVariable = Ext.get("selBinVariableSelection").getValue()
		
		// Add these items to our form parameters.
		formParams["binning"] = "TRUE";
		formParams["numberOfBins"] = numberOfBins;
		formParams["binDistribution"] = binningType;
		//formParams["binVariable"] = binningVariable;

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

