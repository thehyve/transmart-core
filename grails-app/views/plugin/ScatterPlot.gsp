%{--include js lib for scatter plot dynamically--}%
<r:require modules="scatter_plot"/>
<r:layoutResources disposition="defer"/>


<div id="analysisWidget">

    <h2>
        Variable Selection
        <a href='JavaScript:D2H_ShowHelp(1505,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
            <img src="${resource(dir: 'images', file: 'help/helpicon_white.jpg')}" alt="Help"/>
        </a>
    </h2>

    <form id="analysisForm">
        <fieldset class="inputFields">

            %{--Independent variable--}%
            <div class="highDimContainer">
                <h3>Independent Variable</h3>
                <span>Select a continuous variable from the Data Set Explorer Tree and drag it into the box.</span>
                <div id='divIndependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                <div class="highDimBtns">
                    <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divIndependentVariable')">High Dimensional Data</button>
                    <button type="button" onclick="scatterPlotView.clear_high_dimensional_input('divIndependentVariable')">Clear</button>
                </div>
            </div>

            %{--Display independent variable--}%
            <div id="displaydivIndependentVariable" class="independentVars"></div>


            %{--Dependent variable--}%
            <div class="highDimContainer">
                <h3>Dependent Variable</h3>
                <span>Select a continuous variable from the Data Set Explorer Tree and drag it into the box.</span>
                <div id='divDependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                <div class="highDimBtns">
                    <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divDependentVariable')">High Dimensional Data</button>
                    <button type="button" onclick="scatterPlotView.clear_high_dimensional_input('divDependentVariable')">Clear</button>
                </div>
            </div>

            %{--Display independent variable--}%
            <div id="displaydivDependentVariable" class="independentVars"></div>

        </fieldset>

        <fieldset class="toolFields">
            <div>
                <g:checkBox name="logX"/> Perform log<sub>10</sub> transformation on independent variable
            </div>
            <input type="button" value="Run" onClick="scatterPlotView.submit_job(this.form);" class="runAnalysisBtn">
        </fieldset>
    </form>

</div>