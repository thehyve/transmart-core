/**
 * Register drag and drop.
 * Clear out all gobal variables and reset them to blank.
 */
function loadMarkerSelectionView(){
    registerMarkerSelectionDragAndDrop();
    clearHighDimDataSelections('divIndependentVariable');
}

/**
 *  Submitting the heatmap job
 * @param form
 */
function submitMarkerSelectionJob (frm) {

    // get formParams
    var formParams = getFormParameters();

    if (formParams) { // if formParams is not null
        submitJob(formParams);
    }
}

/**
 * get form parameters
 * @param frm
 */
function getFormParameters () {

    var formParameters = {}; // init

    //Use a common function to load the High Dimensional Data params.
    loadCommonHighDimFormObjects(formParameters, "divIndependentVariable");

    // instantiate input elements object with their corresponding validations
    var inputArray = [
        {
            "label" : "High Dimensional Data",
            "el" : Ext.get("divIndependentVariable"),
            "validations" : [
                {type:"REQUIRED"},
                {
                    type:"HIGH_DIMENSIONAL",
                    platform:formParameters["divIndependentVariableType"],
                    pathway:formParameters["divIndependentVariablePathway"]
                }
            ]
        },
        {
            "label" : "Number of Markers",
            "el" : document.getElementById("txtNumberOfMarkers"),
            "validations" : [{type:"REQUIRED"}, {type:"INTEGER", min:1}]
        },
        {
            "label" : "Image Width",
            "el" : document.getElementById("txtImageWidth"),
            "validations" : [{type:"REQUIRED"}, {type:"INTEGER", min:1, max:9000}]
        },
        {
            "label" : "Image Height",
            "el" : document.getElementById("txtImageHeight"),
            "validations" : [{type:"REQUIRED"}, {type:"INTEGER", min:1, max:9000}]
        },
        {
            "label" : "Image Point Size",
            "el" : document.getElementById("txtImagePointsize"),
            "validations" : [{type:"REQUIRED"}, {type:"INTEGER", min:1, max:100}]
        },
        {
            "label" : "Subsets",
            "el" : null,
            "value" : [GLOBAL.CurrentSubsetIDs[1], GLOBAL.CurrentSubsetIDs[2]],
            "validations" : [{type:"MIN_TWO_SUBSETS"}]
        }
    ]

    // define the validator for this form
    var formValidator = new FormValidator(inputArray);

    if (formValidator.validateInputForm()) { // if input files satisfy the validations

        // get values
        var inputConceptPathVar = readConceptVariables("divIndependentVariable");
        var numOfMarkers = inputArray[1].el.value;
        var imageWidth = inputArray[2].el.value;
        var imageHeight = inputArray[3].el.value;
        var imagePointSize = inputArray[4].el.value;

        // assign values to form parameters
        formParameters['jobType'] = 'MarkerSelection';
        formParameters['independentVariable'] = inputConceptPathVar;
        formParameters['variablesConceptPaths'] = inputConceptPathVar;
        formParameters['txtNumberOfMarkers'] = numOfMarkers;
        formParameters['txtImageWidth'] = imageWidth;
        formParameters['txtImageHeight'] = imageHeight;
        formParameters['txtImagePointsize'] = imagePointSize;

    } else { // something is not correct in the validation
        // empty form parameters
        formParameters = null;
        // display the error message
        formValidator.display_errors();
    }

    return formParameters;
}

function loadMarkerSelectionOutput()
{
	$j("#markerSelectionTable").tablesorter(); 
}

/**
 * Clear the variable selection box
 * Clear all selection stored in global variables
 * Clear the selection display
 * @param divName
 */
function clearGroupMarkerSelection(divName)
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

function registerMarkerSelectionDragAndDrop()
{
	//Set up drag and drop for Dependent and Independent variables on the data association tab.
	//Get the Independent DIV
	var independentDiv = Ext.get("divIndependentVariable");
	
	dtgI = new Ext.dd.DropTarget(independentDiv,{ddGroup : 'makeQuery'});
	dtgI.notifyDrop =  dropOntoCategorySelection;
	
}