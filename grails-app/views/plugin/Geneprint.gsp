%{--include js lib for geneprint dynamically--}%
<r:require modules="geneprint"/>
<r:layoutResources disposition="defer"/>

<div id="analysisWidget">

    <h2>
        Variable Selection
    </h2>

    <form id="analysisForm">
        <fieldset class="inputFields">

            %{--High dimensional input--}%
            <div class="highDimContainer">
                <span>Select a High Dimensional Data node from the Data Set Explorer Tree and drag it into the box.</span>
                <div id='divIndependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                <div class="highDimBtns">
                    <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divIndependentVariable', false, false)">High Dimensional Data</button>
                    <button type="button" onclick="geneprintView.clear_high_dimensional_input('divIndependentVariable')">Clear</button>
                </div>
            </div>

            %{--Display independent variable--}%
            <div id="displaydivIndependentVariable" class="independentVars"></div>

            %{--Z-score thresholds--}%
            <label for="txtMrnaThreshold" style="width:225px">mRNA expression z-score threshold ±:</label>
            <input type="text" id="txtMrnaThreshold" value="2.0" style="width:100px"/>
            <label for="txtProteinThreshold" style="width:225px">Protein expression z-score threshold ±:</label>
            <input type="text" id="txtProteinThreshold" value="2.0" style="width:100px"/>
        </fieldset>

        <fieldset class="toolFields">
            <input type="button" value="Run" onClick="geneprintView.submit_job(this.form);" class="runAnalysisBtn">
        </fieldset>
    </form>

</div>
