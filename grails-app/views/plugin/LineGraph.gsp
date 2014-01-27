%{--include js lib for box plot dynamically--}%
<r:require modules="line_graph"/>
<r:layoutResources disposition="defer"/>

<div id="analysisWidget">

    %{--help and title--}%
    <h2>
        Variable Selection
        <a href='JavaScript:D2H_ShowHelp(1291,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
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

                    %{--Time/Measurement Concepts--}%
                    <div class="highDimContainer">
                        <h3>Time/Measurement Concepts</h3>
                        <span class="hd-notes">
                          Drag one or more <strong>numerical</strong> concepts from the tree into the box below.
                          The concepts must come from a data node (Biomarker Data or Clinical Data).
                        </span>
                        <div id='divDependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                        <div class="highDimBtns">
                            <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divDependentVariable')">High Dimensional Data</button>
                            <button type="button" onclick="lineGraphView.clear_high_dimensional_input('divDependentVariable')">Clear</button>
                        </div>
                        <input type="hidden" id="independentVarDataType">
                        <input type="hidden" id="independentPathway">
                    </div>

                    %{--Display dependent variable--}%
                    <div id="displaydivDependentVariable" class="dependentVars"></div>

                </fieldset>
            </div>

            %{-- ************************************************************************************************* --}%
            %{-- Right inputs --}%
            %{-- ************************************************************************************************* --}%

            <div class="right">

                <fieldset class="inputFields">
                    %{-- GroupByVariable variable--}%
                    <div class="highDimContainer">
                        <h3>Group Concepts</h3>
                        <span class="hd-notes">
                          Drag one or more concepts from the tree into the box below to divide the
                          subjects into groups (for example, Treatment Groups). A folder may be dragged
                          in to include all leaf nodes under that folder. Each group will be plotted as a
                          distinct line on the graph.
                        </span>
                        <div id='divGroupByVariable' class="queryGroupIncludeSmall highDimBox"></div>
                        <div class="highDimBtns">
                            <button type="button" onclick="lineGraphView.clear_high_dimensional_input('divGroupByVariable')">Clear</button>
                        </div>
                        <input type="hidden" id="dependentVarDataType">
                        <input type="hidden" id="dependentPathway">
                    </div>

                    %{--Display group variable--}%
                    <div id="displaydivGroupByVariable" class="groupByVariable"></div>
                </fieldset>

            </div>

            <label for="graphType">Graph type</label>
            <select id="graphType">
              <option value="MERR">Mean with error bar</option>
              <option value="MSTD">Mean with standard deviation</option>
              <option value="MEDER">Median with error bar</option>
            </select><br>
            <label for="plotIndividuals">Plot individuals</label><input id="plotIndividuals" type="checkbox" name="plotIndividuals"></input><br>
            <input type="button" value="Run" onClick="lineGraphView.submit_job(this.form);">

        </div>  %{--end container--}%
    </form>

</div>
