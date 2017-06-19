
<script type="text/ng-template" id="linegraph">

    <div ng-controller="LinegraphController">

        <tab-container>

            <workflow-tab tab-name="Fetch Data" disabled="fetch.disabled">
                <concept-box style="display: inline-block"
                             concept-group="fetch.conceptBoxes.highData"
                             type="HD"
                             min="0"
                             max="-1"
                             label="(optional) High Dimensional Variables"
                             tooltip="Select an arbitrary number of high dimensional variables and drag them here.">
                </concept-box>
                <concept-box style="display: inline-block"
                             concept-group="fetch.conceptBoxes.numData"
                             type="LD-numerical"
                             min="0"
                             max="-1"
                             label="(optional) Numerical Variables"
                             tooltip="Select an arbitrary number of numeric variables and drag them here.">
                </concept-box>
                <concept-box style="display: inline-block;"
                             concept-group="fetch.conceptBoxes.catData"
                             type="LD-categorical"
                             min="0"
                             max="-1"
                             label="(optional) Categorical Variables"
                             tooltip="Select an arbitrary number of categoric variables and drag them here.">
                </concept-box>

                <biomarker-selection biomarkers="fetch.selectedBiomarkers"></biomarker-selection>
                <hr class="sr-divider">
                <br/>
                <fetch-button concept-map="fetch.conceptBoxes"
                              loaded="fetch.loaded"
                              running="fetch.running"
                              allowed-cohorts="[1,2]"
                              disabled="fetch.disabled"
                              biomarkers="fetch.selectedBiomarkers">
                </fetch-button>
            </workflow-tab>

            <workflow-tab tab-name="Run Analysis" disabled="runAnalysis.disabled">
                <run-button button-name="Create Plot"
                            store-results-in="runAnalysis.scriptResults"
                            script-to-run="run"
                            arguments-to-use="runAnalysis.params"
                            filename="linegraph.json"
                            running="runAnalysis.running">
                </run-button>
                <capture-plot-button filename="linegraph.svg" target="line-graph"></capture-plot-button>
                <br/>
                <br/>
                <line-graph data="runAnalysis.scriptResults"></line-graph>
            </workflow-tab>

        </tab-container>

    </div>

</script>
