%{--include js lib for heatmap dynamically--}%
<r:require modules="hclust"/>
<r:layoutResources disposition="defer"/>

<div id="analysisWidget">

    <h2>
        Variable Selection
        <a href='JavaScript:D2H_ShowHelp(1506,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
            <img src="${resource(dir: 'images', file: 'help/helpicon_white.jpg')}" alt="Help"/>
        </a>
    </h2>

    <form id="analysisForm">
        <fieldset class="inputFields">

            %{--High dimensional input--}%
            <div class="highDimContainer">
                <span>Select a High Dimensional Data node from the Data Set Explorer Tree and drag it into the box.</span>
                <div id='divIndependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                <div class="highDimBtns">
                    <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divIndependentVariable')">High Dimensional Data</button>
                    <button type="button" onclick="hierarchicalClusteringView.clear_high_dimensional_input('divIndependentVariable')">Clear</button>
                </div>
            </div>

            %{--Display independent variable--}%
            <div id="displaydivIndependentVariable" class="independentVars"></div>

            <label for="txtMaxDrawNumber">Max rows to display:</label>
            <input type="text" id="txtMaxDrawNumber"/>

        </fieldset>

        <fieldset class="toolFields">
            <div>
                <input type="checkbox" id="chkClusterRows" name="doClusterRows" checked="true">
                <span>Apply clustering for rows</span>
            </div>
            <div>
                <input type="checkbox" id="chkClusterColumns" name="doClusterColumns" checked="true">
                <span>Apply clustering for columns</span>
            </div>
            <input type="button" value="Run" onClick="hierarchicalClusteringView.submit_job(this.form);" class="runAnalysisBtn">
        </fieldset>
    </form>

</div>
