%{--include js lib for scatter plot dynamically--}%
<r:require modules="scatter_plot"/>
<r:layoutResources disposition="defer"/>

<div id="analysisWidget">

    <h2>
        Variable Selection
        <a href='JavaScript:D2H_ShowHelp(1512,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
            <img src="${resource(dir: 'images', file: 'help/helpicon_white.jpg')}" alt="Help"/>
        </a>
    </h2>

    <form id="analysisForm">

        <div class="container">

            %{-- ************************************************************************************************* --}%
            %{-- Left inputs --}%
            %{-- ************************************************************************************************* --}%
            <div class="left">
                <fieldset class="inputFields">
                    %{--Independent variable--}%
                    <div class="highDimContainer">
                        <h3>Independent Variable</h3>
                        <span>Select a continuous variable from the Data Set Explorer Tree and drag it into the box.</span>
                        <div id='divIndependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                        <div class="highDimBtns">
                            <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divIndependentVariable', true)">High Dimensional Data</button>
                            <button type="button" onclick="scatterPlotView.clear_high_dimensional_input('divIndependentVariable')">Clear</button>
                        </div>
                        <input type="hidden" id="independentVarDataType">
                        <input type="hidden" id="independentPathway">
                    </div>

                    %{--Display independent variable--}%
                    <div id="displaydivIndependentVariable" class="independentVars"></div>

                </fieldset>
            </div>

            %{-- ************************************************************************************************* --}%
            %{-- Right inputs --}%
            %{-- ************************************************************************************************* --}%
            <div class="right">
                <fieldset class="inputFields">
                    %{--Dependent variable--}%
                    <div class="highDimContainer">
                        <h3>Dependent Variable</h3>
                        <span>Select a continuous variable from the Data Set Explorer Tree and drag it into the box.</span>
                        <div id='divDependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                        <div class="highDimBtns">
                            <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divDependentVariable', true)">High Dimensional Data</button>
                            <button type="button" onclick="scatterPlotView.clear_high_dimensional_input('divDependentVariable')">Clear</button>
                        </div>
                        <input type="hidden" id="dependentVarDataType">
                        <input type="hidden" id="dependentPathway">
                    </div>

                    %{--Display dependent variable--}%
                    <div id="displaydivDependentVariable" class="dependentVars"></div>
                </fieldset>
            </div>

        </div>  %{--end container--}%

        %{-- ************************************************************************************************* --}%
        %{-- Tool Bar --}%
        %{-- ************************************************************************************************* --}%

        <fieldset class="toolFields">
            <div>
                <g:checkBox name="divIndependentVariableLog10"/> Perform log<sub>10</sub> transformation on independent variable
            </div>
            <input type="button" value="Run" onClick="scatterPlotView.submit_job(this.form);" class="runAnalysisBtn">
        </fieldset>

    </form>

</div>
