/**
 * Where everything starts
 */
function loadTableWithFisherView(){
    tableWithFisherView.register_drag_drop();
    tableWithFisherView.clear_high_dimensional_input('divIndependentVariable');
    tableWithFisherView.clear_high_dimensional_input('divDependentVariable');
    tableWithFisherView.toggle_binning_fisher();
}


/**
 * Constructor
 * @constructor
 */
var TableWithFisherView = function () {
    RmodulesView.call(this);
}

/**
 * Inherit RModulesView
 * @type {RmodulesView}
 */
TableWithFisherView.prototype = new RmodulesView();

/**
 * Correct pointer
 * @type {TableWithFisherView}
 */
TableWithFisherView.prototype.constructor = TableWithFisherView;


/**
 * Get form parameters
 * TODO: Refactor the validation to define validation in FormValidator.js instead here
 * @param form
 * @returns {*}
 */
TableWithFisherView.prototype.get_form_params = function (form) {

    var dependentVariableEle = Ext.get("divDependentVariable");
    var independentVariableEle = Ext.get("divIndependentVariable");

    var dependentVariableConceptPath = "";
    var independentVariableConceptPath = "";

    //If the category variable element has children, we need to parse them and concatenate their values.
    if(independentVariableEle.dom.childNodes[0])
    {
        //Loop through the category variables and add them to a comma seperated list.
        for(nodeIndex = 0; nodeIndex < independentVariableEle.dom.childNodes.length; nodeIndex++)
        {
            //If we already have a value, add the seperator.
            if(independentVariableConceptPath != '') independentVariableConceptPath += '|'

            //Add the concept path to the string.
            independentVariableConceptPath += RmodulesView.fetch_concept_path(
                independentVariableEle.dom.childNodes[nodeIndex])
        }
    }

    //If the category variable element has children, we need to parse them and concatenate their values.
    if(dependentVariableEle.dom.childNodes[0])
    {
        //Loop through the category variables and add them to a comma seperated list.
        for(nodeIndex = 0; nodeIndex < dependentVariableEle.dom.childNodes.length; nodeIndex++)
        {
            //If we already have a value, add the seperator.
            if(dependentVariableConceptPath != '') dependentVariableConceptPath += '|'

            //Add the concept path to the string.
            dependentVariableConceptPath += RmodulesView.fetch_concept_path(
                dependentVariableEle.dom.childNodes[nodeIndex])
        }
    }

    //------------------------------------
    //Validation
    //------------------------------------
    //Make sure the user entered some items into the variable selection boxes.
    if(dependentVariableConceptPath == '' && independentVariableConceptPath == '')
    {
        Ext.Msg.alert('Missing input', 'Please drag at least one concept into the independent variable and dependent variable boxes.');
        return;
    }

    if(dependentVariableConceptPath == '')
    {
        Ext.Msg.alert('Missing input', 'Please drag at least one concept into the dependent variable box.');
        return;
    }

    if(independentVariableConceptPath == '')
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
    if((this.isNumerical(dependentNodeList) || this.isHd(dependentNodeList)) && (dependentVariableConceptPath.indexOf("|") != -1))
    {
        Ext.Msg.alert('Wrong input', 'For continuous and high dimensional data, you may only drag one node into the input boxes. The Dependent input box has multiple nodes.');
        return;
    }

    if((this.isNumerical(independentNodeList) || this.isHd(independentNodeList)) && (independentVariableConceptPath.indexOf("|") != -1))
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
    if(GLOBAL.Binning && document.getElementById('EnableBinningDep').checked && Ext.get('variableTypeDep').getValue() == 'Continuous' && ((dependentVariableConceptPath != "" && this.isCategorical(dependentNodeList)) || (this.isHd(dependentNodeList) && window['divDependentVariableSNPType'] == "Genotype" && window['divDependentVariablemarkerType'] == 'SNP')) )
    {
        Ext.Msg.alert('Wrong input', 'There is a categorical input in the Dependent variable box, but you are trying to bin it as if it was continuous. Please alter your binning options or the concept in the Dependent variable box.');
        return;
    }
    if(GLOBAL.Binning && document.getElementById('EnableBinningIndep').checked && Ext.get('variableTypeIndep').getValue() == 'Continuous' && ((independentVariableConceptPath != "" && this.isCategorical(independentNodeList)) || (this.isHd(independentNodeList) && window['divIndependentVariableSNPType'] == "Genotype" && window['divIndependentVariablemarkerType'] == 'SNP')) )
    {
        Ext.Msg.alert('Wrong input', 'There is a categorical input in the Independent variable box, but you are trying to bin it as if it was continuous. Please alter your binning options or the concept in the Independent variable box.');
        return;
    }

    //These tell us if we are binning the dependent or independent box.
    if(GLOBAL.Binning && document.getElementById('EnableBinningIndep').checked) indVariableType = "CAT"
    if(GLOBAL.Binning && document.getElementById('EnableBinningDep').checked) depVariableType = "CAT"

//    console.log(document.getElementById('EnableBinningIndep').checked)
//    console.log(document.getElementById('EnableBinningIndep').checked)
//
//    console.log ("indVariableType", indVariableType)
//    console.log ("depVariableType", depVariableType)

    //If there is a categorical variable in either box (This means either of the lists are empty)
    if(this.isCategorical(dependentNodeList)) depVariableType = "CAT";
    if(this.isCategorical(independentNodeList)) indVariableType = "CAT";

    //The last type of category is if we have high dim data, using SNP(Genotype).
    if(this.isHd(dependentNodeList) && window['divDependentVariableSNPType'] == "Genotype" && window['divDependentVariablemarkerType'] == 'SNP') depVariableType = "CAT";
    if(this.isHd(independentNodeList) && window['divIndependentVariableSNPType'] == "Genotype" && window['divIndependentVariablemarkerType'] == 'SNP') indVariableType = "CAT";

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
    if(this.isCategorical(dependentNodeList) && dependentVariableConceptPath.indexOf("|") == -1)
    {
        Ext.Msg.alert('Wrong input', 'When using categorical variables you must use at least 2. The dependent box only has 1 categorical variable in it.');
        return;
    }

    if(this.isCategorical(independentNodeList) && independentVariableConceptPath.indexOf("|") == -1)
    {
        Ext.Msg.alert('Wrong input', 'When using categorical variables you must use at least 2. The independent box only has 1 categorical variable in it.');
        return;
    }
    //------------------------------------

    var variablesConceptCode = dependentVariableConceptPath + "|" + independentVariableConceptPath;

    var formParams = {dependentVariable:dependentVariableConceptPath,
        independentVariable:independentVariableConceptPath,
        jobType:'TableWithFisher',
        variablesConceptPaths:variablesConceptCode};

    if(!highDimensionalData.load_parameters(formParams)) return false;
    this.load_binning_parameters(formParams);

    return formParams;
}

/**
 * Submit the job
 * @param form
 */
TableWithFisherView.prototype.submit_job = function (form) {

    // get formParams
    var formParams = this.get_form_params(form);
    console.log("formParams ....",formParams)

    if (formParams) { // if formParams is not null
        submitJob(formParams);
    }

}

/**
 * When we change the number of bins in the "Number of Bins" input, we have to
 * change the number of bins on the screen.
 * @param newNumberOfBins
 * @param binningSuffix
 */
TableWithFisherView.prototype.manage_bins_fisher = function (newNumberOfBins, binningSuffix) {

    // This is the row template for a continuous binning row
    var tpl = new Ext.Template(
        '<tr id="binningContinousRow{0}{1}">',
        '<td>Bin {0}</td><td><input type="text" id="txtBin{0}{1}RangeLow" /> - <input type="text" id="txtBin{0}{1}RangeHigh" /></td>',
        '</tr>');
    var tplcat = new Ext.Template(
        '<tr id="binningCategoricalRow{0}{1}">',
        '<td>Bin {0}<div id="divCategoricalBin{0}{1}" class="manualBinningBin"></div></td>',
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
            dropZone.notifyDrop = this.drop_onto_bin; // dont forget to make each
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
    this.update_manual_binning_fisher(binningSuffix);
}

/**
 *
 * @param binningSuffix
 */
TableWithFisherView.prototype.update_manual_binning_fisher = function (binningSuffix) {
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
 * Toggle global binning
 */
TableWithFisherView.prototype.toggle_binning_fisher = function () {
    if ($j("#isBinning").prop('checked') ) {
        GLOBAL.Binning = true;
        $j(".binningDiv").show();
    } else {
        GLOBAL.Binning = false;
        $j(".binningDiv").hide();
    }
    console.log(" GLOBAL.Binning", GLOBAL.Binning)
}

/**
 * Load binning variables
 * @param formParams
 */
TableWithFisherView.prototype.load_binning_parameters = function (formParams)
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

// instantiate table fisher instance
var tableWithFisherView = new TableWithFisherView();
