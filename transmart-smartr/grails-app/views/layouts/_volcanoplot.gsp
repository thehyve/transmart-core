
<script type="text/ng-template" id="volcanoplot">

    <div ng-controller="VolcanoplotController">

        <tab-container>

            <workflow-tab tab-name="Fetch Data" disabled="fetch.disabled">
                <concept-box style="display: inline-block"
                             concept-group="fetch.conceptBoxes.highDimensional"
                             type="HD"
                             min="1"
                             max="-1"
                             label="High Dimensional Variables"
                             tooltip="Select high dimensional data node(s) from the data tree and drag it into the box.
                             The nodes needs to be from the same platform.">
                </concept-box>
                <br/>
                <br/>
                <fetch-button concept-map="fetch.conceptBoxes"
                              loaded="fetch.loaded"
                              running="fetch.running"
                              allowed-cohorts="[2]">
                </fetch-button>
            </workflow-tab>

            <workflow-tab tab-name="Run Analysis" disabled="runAnalysis.disabled">
                <run-button button-name="Create Plot"
                            store-results-in="runAnalysis.scriptResults"
                            script-to-run="run"
                            arguments-to-use="runAnalysis.params"
                            filename="volcanoplot.json"
                            running="runAnalysis.running">
                </run-button>
                <capture-plot-button filename="volcanoplot.svg" target="volcano-plot"></capture-plot-button>
                <br/>
                <br/>
                <volcano-plot data="runAnalysis.scriptResults" width="1000" height="800"></volcano-plot>
            </workflow-tab>

        </tab-container>

    </div>

</script>
