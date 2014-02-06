/*************************************************************************
  * tranSMART - translational medicine data mart
 * 
 * Copyright 2008-2012 Janssen Research & Development, LLC.
 * 
 * This product includes software developed at Janssen Research & Development, LLC.
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software  * Foundation, either version 3 of the License, or (at your option) any later version, along with the following terms:
 * 1.	You may convey a work based on this program in accordance with section 5, provided that you retain the above notices.
 * 2.	You may convey verbatim copies of this program code as you receive it, in any medium, provided that you retain the above notices.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS    * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *
 ******************************************************************/
function submitLogisticRegressionJob(form){
	var dependentVariableConceptCode = readConceptVariables2("divDependentVariable")[0];
	var independentVariableConceptCode = readConceptVariables2("divIndependentVariable")[0];
	var groupByVariableConceptCode = readConceptVariables2("divGroupByVariable")[0];
	var variablesConceptCode = dependentVariableConceptCode+"|"+groupByVariableConceptCode;

    var formParams = {
        jobType: 'LogisticRegression',
        variablesConceptPaths: variablesConceptCode,
        dependentVariable: dependentVariableConceptCode,
        independentVariable: independentVariableConceptCode,
        groupByVariable: groupByVariableConceptCode
    };

///////////////////////////////////////  VAIDATION
	
	if(dependentVariableConceptCode == '')
	{
		Ext.Msg.alert('Missing input!', 'Please drag one concept into the Numeric variable box.');
		return;
	}	
	
	var variableEle = Ext.get("divGroupByVariable");
	var numricVariableEle = Ext.get("divDependentVariable");


	
	//This will tell us the type of nodes drag into Probability box
	var categoryNodeList = createNodeTypeArrayFromDiv(variableEle,"setnodetype")
    var numericNodeList = createNodeTypeArrayFromDiv(numricVariableEle,"setnodetype")

	//Across Trial/Navigate by study validation.
	//This will tell us which table the nodes came from. This is important because it tells us if they are modifier nodes or regular concept codes. We use this information for validation and for passing to the jobs functions.
	var categoryNodeType = createNodeTypeArrayFromDiv(variableEle,"concepttablename")
	var numericNodeType = createNodeTypeArrayFromDiv(numricVariableEle,"concepttablename")
	
	//Grab the child node.
	//var child = variableEle.dom.childNodes[i];
	
	//This tells us whether it is a numeric or character node.
	//var val = child.attributes.oktousevalues;
	
	if(categoryNodeType.length > 1)
	{
		Ext.Msg.alert('Wrong input', 'The Category input box has nodes from both the \'Navigate By Study\' tree and the \'Across Trial\' tree. Please only use nodes of the same type. ');
		return;		
	}	
	//If we are not a numeric leaf node, than
	//Make sure user entered a group and a concept
	
	if(categoryNodeList.length > 1)
	{
		Ext.Msg.alert('Wrong input', 'You may only drag nodes of the same type (Continuous,Categorical) into the input box. The Probability Concepts input box has multiple types.');
		return;		
	}			

	//Combine the different arrays so we can make sure the type matches across all input boxes.
	var finalNodeType = []

	//This is what we use to determine if we are running a modifier_cd analysis or a concept_cd analysis.
	var codeType
	
	if(categoryNodeType[0] && categoryNodeType[0] != "null") finalNodeType.push(categoryNodeType[0])
	
	//Distinct this final list.
	finalNodeType = finalNodeType.unique()
	
	if(finalNodeType.length > 1)
	{
		Ext.Msg.alert('Wrong input', 'You have selected inputs from different ontology trees, please only select nodes from the \'Navigate By Study\' or \'Across Trial\' tree.');
		return;			
	}
	if((categoryNodeList[0] == 'valueicon' || categoryNodeList[0] == 'hleaficon') && (groupByVariableConceptCode.indexOf("|") != -1))
	{
		Ext.Msg.alert('Wrong input', 'For continuous data, you may only drag one node into the input boxes. The Probability input box has multiple nodes.');
		return;		
	}	
	if((numericNodeList[0] == 'valueicon' || numericNodeList[0] == 'hleaficon') && (dependentVariableConceptCode.indexOf("|") != -1))
	{
		Ext.Msg.alert('Wrong input', 'For continuous data, you may only drag one node into the input boxes. The Numeric input box has multiple nodes.');
		return;		
	}	
	//If its categorical value than make sure you have atleast 2 values
		if(groupByVariableConceptCode == ''  || (  categoryNodeList[0] != 'valueicon' && variableEle.dom.childNodes.length < 2))
	{
		Ext.Msg.alert('Missing input!', 'If categorical concept, than please drag at least two categorical concept into the Probability  Concepts variable box.');
		return;
	}
	
		
		//If its categorical value and its more than 2 values, than make sure they are binned manually
		if(categoryNodeList[0] != 'valueicon' && variableEle.dom.childNodes.length > 2  && !GLOBAL.Binning)
	{
		Ext.Msg.alert('Wrong input!', 'For more than 2 categorical concepts,  please enable binning and use manual binning to group the concepts into 2 groups');
		return;
	}
	
	//If its continuous value, than make sure they are binned 
	if(categoryNodeList[0] == 'valueicon'  && !GLOBAL.Binning)
	{
	Ext.Msg.alert('Wrong input!', 'For continuous data,  please enable binning and bin the concepts into 2 groups');
	return;
	}

	//If binning is enabled and we try to bin a categorical value as a continuous, throw an error.
	if(categoryNodeList[0] != 'valueicon'  && GLOBAL.Binning  && Ext.get('variableType').getValue() == 'Continuous')
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
	//If binning is enabled and the user is trying to categorically bin a continuous variable, alert them.
	if(GLOBAL.Binning && Ext.get('variableType').getValue() != 'Continuous' && (categoryNodeList[0] == 'valueicon'))
	{
		Ext.Msg.alert('Wrong input', 'You cannot use categorical binning with a continuous variable. Please alter your binning options or the concept in the Probablity input  box.');
		return;			
	}	
	
	loadBinningParametersLogisticRegression(formParams);
	submitJob(formParams);
}

function loadLogisticRegressionView(){
	registerLogisticRegressionDragAndDrop();
}

function clearGroupLine(divName)
{
	//Clear the drag and drop div.
	var qc = Ext.get(divName);
	
	for(var i=qc.dom.childNodes.length-1;i>=0;i--)
	{
		var child=qc.dom.childNodes[i];
		qc.dom.removeChild(child);
	}	

}
function registerLogisticRegressionDragAndDrop()
{
	//Set up drag and drop for Dependent and Independent variables on the data association tab.

	//Get the Dependent DIV.
	var dependentDiv = Ext.get("divDependentVariable");
	dtgD = new Ext.dd.DropTarget(dependentDiv,{ddGroup : 'makeQuery'});
	dtgD.notifyDrop =  dropNumericOntoCategorySelection;
	
	//Get the group by div
	var groupByDiv = Ext.get("divGroupByVariable");
	dtgG = new Ext.dd.DropTarget(groupByDiv, {ddGroup: 'makeQuery'});
	dtgG.notifyDrop = dropOntoCategorySelection;
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


function updateManualBinningLogisticRegression(){
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
			setupCategoricalItemsList("divGroupByVariable","divCategoricalItems");
		}
	}
}




/**
 * When we change the number of bins in the "Number of Bins" input, we have to
 * change the number of bins on the screen.
 */
function manageBinsLogisticRegression(newNumberOfBins) {

	// This is the row template for a continousBinningRow.
	var tpl = new Ext.Template(
			'<tr id="binningContinousRow{0}">',
			'<td>Bin {0}</td><td><input type="text" id="txtBin{0}RangeLow" title="Low Range" /> - <input type="text" id="txtBin{0}RangeHigh" title="High Range" /></td>',
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
	updateManualBinningLogisticRegression();
}



function dropOntoBin(source, e, data) {
	this.el.appendChild(data.ddel);
	// Ext.dd.Registry.register(data.ddel, {el : data.ddel});
	return true;
}
function loadBinningParametersLogisticRegression(formParams)
{
	
	//These default to FALSE
	formParams["binning"] = "FALSE";
	formParams["manualBinning"] = "FALSE";
	
	// Gather the data from the optional binning items, if we had selected to
	// enable binning.
	if (GLOBAL.Binning) {
		// Get the number of bins the user entered.
		var numberOfBins = 2 //Ext.get("txtNumberOfBins").getValue()

		// Get the value from the dropdown that specifies the type of
		// binning.
		var binningType = Ext.get("selBinDistribution").getValue()

		//Get the value from the dropdown that tells us which variable to bin.
		var binningVariable = "IND" //Ext.get("selBinVariableSelection").getValue()
		
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

