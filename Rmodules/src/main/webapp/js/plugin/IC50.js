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
 * Constructor
 * @constructor
 */
var Ic50View = function () {
    RmodulesView.call(this);
}

/**
 * Inherit RModulesView
 * @type {RmodulesView}
 */
Ic50View.prototype = new RmodulesView();

/**
 * Correct pointer
 * @type {Ic50View}
 */
Ic50View.prototype.constructor = Ic50View;


Ic50View.prototype.submitIC50Job = function(form){
	
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
	if(!this.isCategorical(cellNodeList))
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

    console.log("formParams ....",formParams)

    submitJob(formParams);
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

// instantiate IC50 view instance
var ic50view = new Ic50View();
