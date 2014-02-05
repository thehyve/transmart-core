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

function submitTableWithForestJob(form){
	//Gather an ExtJS object for each of the input elements.
	var dependentVariableEle = Ext.get("divDependentVariable");
	var referenceVariableEle = Ext.get("divReferenceVariable");
	var independentVariableEle = Ext.get("divIndependentVariable");
	var stratificationVariableEle = Ext.get("divStratificationVariable");
	
	var tempReturnArray = ""; 
	
	//Pull the values out of the form inputs.
	tempReturnArray = readConceptVariables2("divDependentVariable");
	var dependentVariableConceptCode = tempReturnArray[0];
	var dependentVariableCode = tempReturnArray[1];	
	
	tempReturnArray = readConceptVariables2("divIndependentVariable");
	var independentVariableConceptCode = tempReturnArray[0];
	var independentVariableCode = tempReturnArray[1];
	
	tempReturnArray = readConceptVariables2("divReferenceVariable");
	var referenceVariableConceptCode = tempReturnArray[0];
	var referenceVariableCode = tempReturnArray[1];
	
	tempReturnArray = readConceptVariables2("divStratificationVariable");
	var stratificationVariableConceptCode = tempReturnArray[0];
	var stratificationVariableCode = tempReturnArray[1];	
	
	//------------------------------------
	//Validation
	//------------------------------------
	//Validate that these boxes have at least one value in them.
	if(!(detectMissingInput(dependentVariableConceptCode, 'dependent'))) return;
	if(!(detectMissingInput(independentVariableConceptCode, 'independent'))) return;
	if(!(detectMissingInput(referenceVariableConceptCode, 'control or reference'))) return;

	//forest plot test requires three categorical variables.
	var depVariableType = "";
	var indVariableType = "";
	var refVariableType = "";
	var statVariableType = "";
	
	//Loop through the dependent variable box and find the the of nodes in the box.
	var dependentNodeList = createNodeTypeArrayFromDiv(dependentVariableEle,"setnodetype")
	var independentNodeList = createNodeTypeArrayFromDiv(independentVariableEle,"setnodetype")
	var referenceNodeList = createNodeTypeArrayFromDiv(referenceVariableEle, "setnodetype")
	var stratificationNodeList = createNodeTypeArrayFromDiv(stratificationVariableEle, "setnodetype")
	
	//If the user dragged multiple node types into an input box, throw an error.
	if(!(detectMultipleNodeTypes(dependentNodeList, 'dependent'))) return;
	if(!(detectMultipleNodeTypes(independentNodeList, 'independent'))) return;
	if(!(detectMultipleNodeTypes(referenceNodeList, 'control or reference'))) return;
	if(!(detectMultipleNodeTypes(stratificationNodeList, 'stratification'))) return;

	//For the valueicon and hleaficon nodes (Value nodes and HDD nodes), you can only put one in a given input box.
	if(!(detectMultipleValueNodes(dependentNodeList, dependentVariableConceptCode, 'Dependent'))) return;
	if(!(detectMultipleValueNodes(independentNodeList, independentVariableConceptCode, 'Independent'))) return;
	if(!(detectMultipleValueNodes(referenceNodeList, referenceVariableConceptCode, 'Reference'))) return;
	if(!(detectMultipleValueNodes(stratificationNodeList, stratificationVariableConceptCode, 'Stratification'))) return;
	
	//Detect when the dependent variable has more than 2 values and binning isn't enabled.
	if(!(detectMultipleCategoricalNodesWithoutBinning(dependentNodeList, dependentVariableConceptCode, 'Dependent', 2, GLOBAL.Binning && document.getElementById('EnableBinningDep').checked))) return;
	
	//Detect when the dependent variable box has 2 values and binning isn't enabled and the checkbox is checked.
	if(!(detectMultipleCategoricalNodesWithoutBinningAndGroupCheckboxChecked(dependentNodeList, dependentVariableConceptCode, 'Dependent', 2, GLOBAL.Binning && document.getElementById('EnableBinningDep').checked, document.getElementById('chkAssumeNonEvent').checked))) return;
				
	//Detect when the dependent variable box has 2 bins and the checkbox is checked.
	if(!(detectMultipleBinsWithGroupCheckboxChecked(GLOBAL.Binning && document.getElementById('EnableBinningDep').checked, document.getElementById("txtNumberOfBinsDep").value, document.getElementById('chkAssumeNonEvent').checked))) return;
		
	//Detect when the dependent variable box has only one bin and the checkbox is not checked.	
	if(!(detectOneBinGroupCheckBoxUnchecked(GLOBAL.Binning && document.getElementById('EnableBinningDep').checked, document.getElementById("txtNumberOfBinsDep").value, document.getElementById('chkAssumeNonEvent').checked))) return;
	
	//Detect when either Independent or Reference variables have more than one entry but binning is not enabled.
	if(!(detectMultipleCategoricalNodesWithoutBinning(independentNodeList, independentVariableConceptCode, 'Independent', 1, GLOBAL.Binning && document.getElementById('EnableBinningIndep').checked))) return;
	if(!(detectMultipleCategoricalNodesWithoutBinning(referenceNodeList, referenceVariableConceptCode, 'Reference', 1, GLOBAL.Binning && document.getElementById('EnableBinningReference').checked))) return;
	
	//Detect if only one categorical node was dragged into the stratification box.
	if(!(detectSingleCategoricalNode(stratificationNodeList, stratificationVariableConceptCode, 'Stratification'))) return;
	
	//Detect if we are binning the stratification variable input box and are only specifying one bin.
	if(!(detectOneBinGroup(GLOBAL.Binning && document.getElementById('EnableBinningStratification').checked, document.getElementById("txtNumberOfBinsStratification").value, 'stratification'))) return;
	
	//If binning is enabled, we are doing categorical and the manual binning checkbox is not checked, alert the user.
	if(!(detectCatBinningWithoutManual(GLOBAL.Binning, 'variableTypeIndep', 'chkManualBinIndep', 'EnableBinningIndep' , 'Independent' ))) return;
	if(!(detectCatBinningWithoutManual(GLOBAL.Binning, 'variableTypeReference', 'chkManualBinReference', 'EnableBinningReference', 'Reference' ))) return;
	if(!(detectCatBinningWithoutManual(GLOBAL.Binning, 'variableTypeDep', 'chkManualBinDep', 'EnableBinningDep', 'Dependent' ))) return;
	if(!(detectCatBinningWithoutManual(GLOBAL.Binning, 'variableTypeStratification', 'chkManualBinStratification', 'EnableBinningStratification', 'Stratification' ))) return;
	
	//If binning is enabled and we try to bin a categorical value as a continuous, throw an error.
	if(!(detectCatBinnedAsCont(GLOBAL.Binning, 'EnableBinningIndep', 'variableTypeIndep', independentVariableConceptCode, independentNodeList, 'divIndependentVariableSNPType', 'divIndependentVariablemarkerType', 'Independent' ))) return;
	if(!(detectCatBinnedAsCont(GLOBAL.Binning, 'EnableBinningDep', 'variableTypeDep', dependentVariableConceptCode, dependentNodeList, 'divDependentVariableSNPType', 'divDependentVariablemarkerType', 'Dependent' ))) return;
	if(!(detectCatBinnedAsCont(GLOBAL.Binning, 'EnableBinningReference', 'variableTypeReference', referenceVariableConceptCode, referenceNodeList, 'divReferenceVariableSNPType', 'divReferenceVariablemarkerType', 'Reference' ))) return;
	if(!(detectCatBinnedAsCont(GLOBAL.Binning, 'EnableBinningStratification', 'variableTypeStratification', stratificationVariableConceptCode, stratificationNodeList, 'divStratificationVariableSNPType', 'divStratificationVariablemarkerType', 'Stratification' ))) return;
	
	//These tell us if we are binning the dependent or independent box.
	if(GLOBAL.Binning && document.getElementById('EnableBinningIndep').checked) indVariableType = "CAT"
	if(GLOBAL.Binning && document.getElementById('EnableBinningDep').checked) depVariableType = "CAT"
	if(GLOBAL.Binning && document.getElementById('EnableBinningReference').checked) refVariableType = "CAT"
	if(GLOBAL.Binning && document.getElementById('EnableBinningStratification').checked) statVariableType = "CAT"
		
	//If there is a categorical variable in either box (This means either of the lists are empty)
	if(!dependentNodeList[0] || dependentNodeList[0] == "null") depVariableType = "CAT";
	if(!independentNodeList[0] || independentNodeList[0] == "null") indVariableType = "CAT";
	if(!referenceNodeList[0] || referenceNodeList[0] == "null") refVariableType = "CAT";
	if(!stratificationNodeList[0] || stratificationNodeList[0] == "null") statVariableType = "CAT";

	//The last type of category is if we have high dim data, using SNP(Genotype).
	if(dependentNodeList[0] == 'hleaficon' && window['divDependentVariableSNPType'] == "Genotype" && window['divDependentVariablemarkerType'] == 'SNP') depVariableType = "CAT";
	if(independentNodeList[0] == 'hleaficon' && window['divIndependentVariableSNPType'] == "Genotype" && window['divIndependentVariablemarkerType'] == 'SNP') indVariableType = "CAT";
	if(referenceNodeList[0] == 'hleaficon' && window['divReferenceVariableSNPType'] == "Genotype" && window['divReferenceVariablemarkerType'] == 'SNP') refVariableType = "CAT";
	if(stratificationNodeList[0] == 'hleaficon' && window['divStratificationVariableSNPType'] == "Genotype" && window['divStratificationVariablemarkerType'] == 'SNP') statVariableType = "CAT";
	
	//Check to make sure we have two categorical values.
	if(!(depVariableType=="CAT"))
	{
		Ext.Msg.alert('Wrong input', 'Forest Plot requires a categorical variable in the dependent variable box.');
		return;		
	}
	
	if(!(indVariableType=="CAT"))
	{
		Ext.Msg.alert('Wrong input', 'Forest Plot requires a categorical variable in the independent variable box.');
		return;		
	}
	
	if(!(refVariableType=="CAT"))
	{
		Ext.Msg.alert('Wrong input', 'Forest Plot requires a categorical variable in the reference variable box');
		return;		
	}	
	
	if(stratificationVariableConceptCode != '' && !(statVariableType=="CAT"))
	{
		Ext.Msg.alert('Wrong input', 'Forest Plot requires a categorical variable in the stratification variable box.');
		return;		
	}	
	
	//If the dependent node list is empty but we have a concept in the box (Meaning we dragged in categorical items) and there is only one item in the box, alert the user. 
	if((!dependentNodeList[0] || dependentNodeList[0] == "null") && dependentVariableConceptCode.indexOf("|") == -1 && document.getElementById('chkAssumeNonEvent').checked == false)
	{
		Ext.Msg.alert('Wrong input', 'When using categorical variables you must use at least 2. The dependent box only has 1 categorical variable in it.');
		return;			
	}
	//------------------------------------
	
	var statisticType=document.getElementById("statistic.type").value;
	var nonEventCheckbox = document.getElementById("chkAssumeNonEvent").checked;
	
	/////////////////////////////////////////
	//Combine the different arrays so we can make sure the type matches across all input boxes.
	var finalNodeType = []

	//This is what we use to determine if we are running a modifier_cd analysis or a concept_cd analysis.
	var codeType
	
	var independentNodeType 	= createNodeTypeArrayFromDiv(independentVariableEle,"concepttablename")
	var dependentNodeType 		= createNodeTypeArrayFromDiv(dependentVariableEle,"concepttablename")
	var stratificationNodeType 	= createNodeTypeArrayFromDiv(stratificationVariableEle,"concepttablename")
	var referenceNodeType 		= createNodeTypeArrayFromDiv(referenceVariableEle,"concepttablename")
	
	if(independentNodeType[0] && independentNodeType[0] != "null") finalNodeType.push(independentNodeType[0]) 
	if(dependentNodeType[0] && dependentNodeType[0] != "null") finalNodeType.push(dependentNodeType[0])
	if(stratificationNodeType[0] && stratificationNodeType[0] != "null") finalNodeType.push(stratificationNodeType[0])
	if(referenceNodeType[0] && referenceNodeType[0] != "null") finalNodeType.push(referenceNodeType[0])
	
	//Distinct this final list.
	finalNodeType = finalNodeType.unique()
	
	if(finalNodeType.length > 1)
	{
		Ext.Msg.alert('Wrong input', 'You have selected inputs from different ontology trees, please only select nodes from the \'Navigate By Study\' or \'Across Trial\' tree.');
		return;			
	}
	
	if(finalNodeType[0] == "CONCEPT_DIMENSION")
	{
		codeType = "Concept"
	}
	
	if(finalNodeType[0] == "MODIFIER_DIMENSION")
	{
		codeType = "Modifier"
	}
	
	var variablesConceptCode = ""
	
	if(codeType == "Concept")
	{
		//Create a string of all the concept paths that we need to convert to codes.
		variablesConceptCode = dependentVariableConceptCode + "|" + independentVariableConceptCode + "|" + referenceVariableConceptCode;
		if(stratificationVariableConceptCode) variablesConceptCode += "|" + stratificationVariableConceptCode
		
		//Sloppy, but for now reassign the codes if we want the correct concept path.
		dependentVariableCode = dependentVariableConceptCode
		independentVariableCode = independentVariableConceptCode
		referenceVariableCode = referenceVariableConceptCode
		stratificationVariableCode = stratificationVariableConceptCode		
		
		GLOBAL.codeType = "Concept"
	}
	else if(codeType == "Modifier")
	{
		//Create a string of all the modifier codes so we can put them in the clinical data query.
		variablesConceptCode = dependentVariableCode+"|"+independentVariableCode + "|" + referenceVariableCode;
		if(stratificationVariableCode) variablesConceptCode += "|" + stratificationVariableCode
		
		dependentVariableCode = dependentVariableConceptCode
		independentVariableCode = independentVariableConceptCode	
		referenceVariableCode = referenceVariableConceptCode
		stratificationVariableCode = stratificationVariableConceptCode			
		
		GLOBAL.codeType = "Modifier"
	}	
	/////////////////////////////////////////	
	
	var formParams = {dependentVariable:dependentVariableConceptCode,
			independentVariable:independentVariableConceptCode,
			referenceVariable:referenceVariableConceptCode,
			stratificationVariable:stratificationVariableConceptCode,
			jobType:'ForestPlot',
			codeType : codeType,			
			variablesConceptPaths:variablesConceptCode,
			statisticType:statisticType,
			nonEventCheckbox:nonEventCheckbox};
	
	if(!loadHighDimensionalParameters(formParams)) return false;
	if(!loadHighDimensionalParametersGeneral(formParams, 'Reference')) return false;
	if(!loadHighDimensionalParametersGeneral(formParams, 'Stratification')) return false;
	
	loadBinningParametersForest(formParams);
	
	//After we load the binning parameters, check to see if categorical binning is enabled for the Independent or Control boxes. If it is we lump the inputs together as if the user dragged them all into one box. 
	if(GLOBAL.Binning && document.getElementById('EnableBinningIndep').checked && Ext.get("variableTypeIndep").getValue() == 'Categorical')
	{

		formParams["manualBinningIndep"] = "TRUE";
		formParams["binRangesIndep"] = "bin1" + "<>" + independentVariableConceptCode.replace("|","<>")
		formParams["variableTypeIndep"] = "Categorical";		
		
	}
	
	if(GLOBAL.Binning && document.getElementById('EnableBinningReference').checked && Ext.get("variableTypeReference").getValue() == 'Categorical')
	{
		formParams["manualBinningReference"] = "TRUE";
		formParams["binRangesReference"] = "bin1" + "<>" + referenceVariableConceptCode.replace("|","<>")
		formParams["variableTypeReference"] = "Categorical";			
	}
	
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

function loadForestPlotView(){
	registerForestDragAndDrop();
	clearGroupForest('divIndependentVariable');
	clearGroupForest('divReferenceVariable');
	clearGroupForest('divDependentVariable');
	clearGroupForest('divstratificationVariable');
}

function clearGroupForest(divName) {
	// Clear the drag and drop div.
	var qc = Ext.get(divName);

	for ( var i = qc.dom.childNodes.length - 1; i >= 0; i--) {
		var child = qc.dom.childNodes[i];
		qc.dom.removeChild(child);
	}

	clearHighDimDataSelections(divName);
	clearSummaryDisplay(divName);
}

function toggleBinningForest() {
	// Change the Binning flag.
	GLOBAL.Binning = !GLOBAL.Binning;

	// Change the toggle button text.
	if (GLOBAL.Binning) 
	{
		//Toggle the div with the binning options.		
		document.getElementById('divBinning').style.display = '';
		
		document.getElementById('BinningToggle').value = "Disable"
		
		//Some of the elements on the form have unique binning options, set them up here.
		manageBinsForest(1,'Indep');
		manageBinsForest(1,'Reference');
		
	} else 
	{
		//Toggle the div with the binning options.		
		document.getElementById('divBinning').style.display='none';
		
		document.getElementById('BinningToggle').value = "Enable"
	}
}

function updateManualBinningForest(binningSuffix) {
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
			else if(binningSuffix=="Reference")
			{
				setupCategoricalItemsList("divReferenceVariable","divCategoricalItems" + binningSuffix);
			}
			else if(binningSuffix=="Stratification")
			{
				setupCategoricalItemsList("divStratificationVariable","divCategoricalItems" + binningSuffix);
			}			
			
		}
	}
}

/**
 * When we change the number of bins in the "Number of Bins" input, we have to
 * change the number of bins on the screen.
 */
function manageBinsForest(newNumberOfBins,binningSuffix) {
	
	//Get the number of old bins.
	var oldBinningNumber = Ext.get('txtNumberOfBins' + binningSuffix);
	
	// This is the row template for a continousBinningRow.
	var tpl = new Ext.Template(
			'<tr id="binningContinousRow{0}{1}">',
			'<td>Bin {0}</td><td><input type="text" id="txtBin{0}{1}RangeLow" size="5" title="Low Range" /> - <input type="text" id="txtBin{0}{1}RangeHigh" size="5" title="High Range" />&nbsp;&nbsp;&nbsp;</td>',
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
	if (newNumberOfBins < oldBinningNumber) {
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
	updateManualBinningForest(binningSuffix);
}


function registerForestDragAndDrop()
{
	
	//Set the boxes to have the jstree-drop class.
	jQuery("#divDependentVariable") //.addClass("jstree-drop")
	jQuery("#divIndependentVariable")//.addClass("jstree-drop")
	jQuery("#divReferenceVariable")  //.addClass("jstree-drop")
	jQuery("#divStratificationVariable")  //.addClass("jstree-drop")
	
	//Set up drag and drop for Dependent and Independent variables on the data association tab.
	//Get the Dependent DIV.
	var dependentDiv = Ext.get("divDependentVariable");
	
	//Get the Independent DIV
	var independentDiv = Ext.get("divIndependentVariable");
	
	//Get the Reference DIV
	var referenceDiv = Ext.get("divReferenceVariable");
	
	//Get the stratification DIV
	var stratificationDiv = Ext.get("divStratificationVariable");
	
	//Add the drop targets and handler function.
	dtgD = new Ext.dd.DropTarget(dependentDiv,{ddGroup : 'makeQuery'});
	//dtgD.notifyDrop =  function(source, e, data){dropOntoCategorySelection(source, e, data, this.el, '3')};
    dtgD.notifyDrop = dropOntoCategorySelection;
	
	//Add the drop targets and handler function.
	dtgD = new Ext.dd.DropTarget(referenceDiv,{ddGroup : 'makeQuery'});
	//dtgD.notifyDrop =  function(source, e, data){dropOntoCategorySelection(source, e, data, this.el, '3')};
    dtgD.notifyDrop = dropOntoCategorySelection;
	
	dtgI = new Ext.dd.DropTarget(independentDiv,{ddGroup : 'makeQuery'});
	//dtgI.notifyDrop =  function(source, e, data){dropOntoCategorySelection(source, e, data, this.el, '3')};
    dtgI.notifyDrop = dropOntoCategorySelection;
	
	dtgI = new Ext.dd.DropTarget(stratificationDiv,{ddGroup : 'makeQuery'});
	//dtgI.notifyDrop =  function(source, e, data){dropOntoCategorySelection(source, e, data, this.el, '3')};
    dtgI.notifyDrop = dropOntoCategorySelection;
	
}

function validateDependentBins(numberOfBins)
{
	if(numberOfBins > 2)
	{
		document.getElementById("txtNumberOfBinsDep").value = 2;
		Ext.Msg.alert('Incorrect Number of Bins', 'The maximum number of bins you can specify for the Dependent variable is 2.');
		return false;
	}
	
	if(numberOfBins != 1 && document.getElementById("chkAssumeNonEvent").checked == true)
	{
		document.getElementById("txtNumberOfBinsDep").value = 1;
		Ext.Msg.alert('Incorrect Number of Bins', 'You may only specify 1 bin when grouping subjects in the Dependent variable box.');
		return false;
	}

}

function loadBinningParametersForest(formParams)
{
	// These default to FALSE
	formParams["binningDep"] = "FALSE";
	formParams["binningIndep"] = "FALSE";
	formParams["binningReference"] = "FALSE";
	formParams["binningStratification"] = "FALSE";
	formParams["manualBinningDep"] = "FALSE";
	formParams["manualBinningIndep"] = "FALSE";
	formParams["manualBinningReference"] = "FALSE";
	formParams["manualBinningStratification"] = "FALSE";
	
	// Gather the data from the optional binning items, if we had selected to enable binning. Only gather if one of the binning sections is checked.
	if (GLOBAL.Binning && (document.getElementById('EnableBinningDep').checked || document.getElementById('EnableBinningIndep').checked || document.getElementById('EnableBinningStratification').checked || document.getElementById('EnableBinningReference').checked)) 
	{
		//This is the list of suffixes we used on the HTML input IDs.
		var suffixList = ['Dep','Indep', 'Reference', 'Stratification'];
		
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
		if (GLOBAL.ManualBinning && (document.getElementById('chkManualBinDep').checked || document.getElementById('chkManualBinIndep').checked || document.getElementById('chkManualBinReference').checked || document.getElementById('chkManualBinStratification').checked)) 
		{
			//Loop through the list of HTML suffixes.
			for(j=0;j<suffixList.size();j++)
			{
				//Grab the suffix.
				var currentSuffix = suffixList[j];
				
				//Only do this if the checkbox for binning this variable manually is checked.
				if(document.getElementById('chkManualBin' + currentSuffix).checked && document.getElementById('EnableBinning' + currentSuffix).checked)
				{
					// Get a bar separated list of bins and their ranges.
					var binRanges = ""
	
					// Loop over each row in the HTML table.
					var variableType = Ext.get('variableType' + currentSuffix).getValue();
					var numberOfBins = Ext.get('txtNumberOfBins' + currentSuffix).getValue();
					
					//Depending on the variable type, build the binning string differently. 
					if (variableType == "Continuous") 
					{
						for (i = 1; i <= numberOfBins; i++) 
						{
							binRanges += "bin" + i + ","
							binRanges += Ext.get('txtBin' + i + currentSuffix + 'RangeLow').getValue() + ","
							binRanges += Ext.get('txtBin' + i + currentSuffix + 'RangeHigh').getValue()	+ "|"
						}
					} 
					else 
					{
						for (i = 1; i <= numberOfBins; i++) 
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

