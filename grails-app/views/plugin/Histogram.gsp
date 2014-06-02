%{--include js lib for heatmap dynamically--}%
<r:require modules="histogram"/>
<r:layoutResources disposition="defer"/>

<div id="analysisWidget">

    <h2>
        Histogram
    </h2>

    <form id="analysisForm">
        <fieldset class="inputFields">
            <div class="highDimContainer">
                <span>Select a continuous variable from the Data Set Explorer Tree and drag it into the box.</span>
                <div id='divDataNode' class="queryGroupIncludeSmall highDimBox"></div>
                <div class="highDimBtns">
                    <button type="button" onclick="histogramView.clear_high_dimensional_input('divDataNode');">Clear</button>
                </div>
            </div>
        </fieldset>
        <fieldset class="toolFields">
            <input type="button" value="Run" onClick="histogramView.submit_job(this.form);" class="runAnalysisBtn">
        </fieldset>
    </form>

</div>
