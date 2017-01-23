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
                          %{--Drag one or more <strong>numerical</strong> concepts from the tree into the box below or--}%
                          %{--high dimensional node. The concepts must come from a data node (Biomarker Data or Clinical--}%
                        %{--Data).--}%

                          Drag one or multiple numerical or high dimensional nodes from the tree into box below.

                        </span>
                        <div id='divDependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                        <div class="highDimBtns">
                            <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divDependentVariable', true)">High Dimensional Data</button>
                            <button type="button" onclick="lineGraphView.clear_high_dimensional_input('divDependentVariable')">Clear</button>
                        </div>
                        <input type="hidden" id="dependentVarDataType">
                        <input type="hidden" id="dependentPathway">
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
                          %{--Drag one or more concepts from the tree into the box below to divide the--}%
                          %{--subjects into groups (for example, Treatment Groups). A folder may be dragged--}%
                          %{--in to include all leaf nodes under that folder. Each group will be plotted as a--}%
                          %{--distinct line on the graph.--}%
                          Drag one or multiple nodes from the tree into box below. Node should be categorical
                          (Numerical or High Dimensional with binning).
                        </span>
                        <div id='divGroupByVariable' class="queryGroupIncludeSmall highDimBox"></div>
                        <div class="highDimBtns">
                            <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divGroupByVariable', true)">High Dimensional Data</button>
                            <button type="button" onclick="lineGraphView.clear_high_dimensional_input('divGroupByVariable')">Clear</button>
                        </div>
                        <input type="hidden" id="groupByVarDataType">
                        <input type="hidden" id="groupByPathway">
                    </div>

                    %{--Display group variable--}%
                    <div id="displaydivGroupByVariable" class="groupByVariable"></div>

                    %{--Binning options--}%
                    <fieldset class="binningDiv">

                        <label for="variableType">Variable Type</label>
                        <select id="variableType" onChange="lineGraphView.update_manual_binning();">
                            <option value="Continuous">Continuous</option>
                            <option value="Categorical">Categorical</option>
                        </select>

                        <label for="txtNumberOfBins">Number of Bins:</label>
                        <input type="text" id="txtNumberOfBins" onChange="lineGraphView.manage_bins(this.value);" value="4" />

                        <label for="selBinDistribution">Bin Assignments</label>
                        <select id="selBinDistribution">
                            <option value="EDP">Evenly Distribute Population</option>
                            <option value="ESB">Evenly Spaced Bins</option>
                        </select>

                        <div class="chkpair">
                            <input type="checkbox" id="chkManualBin" onClick="lineGraphView.manage_bins(document.getElementById('txtNumberOfBins').value);"> Manual Binning
                        </div>

                        %{-- Manual binning continuous variable --}%
                        <div id="divManualBinContinuous" style="display: none;">
                            <table id="tblBinContinuous">
                                <tr>
                                    <td>Bin Name</td>
                                    <td colspan="2">Range</td>
                                </tr>
                            </table>
                        </div>

                        %{-- Manual binning categorical variable --}%
                        <div id="divManualBinCategorical" style="display: none;">
                            <table>
                                <tr>
                                    <td style="vertical-align: top;">Categories
                                        <div id='divCategoricalItems' class="manualBinningCategories"></div>
                                    </td>
                                    <td style="vertical-align: top;"><br />
                                        <br><span class="minifont">&laquo;Drag To Bin&raquo</span></td>
                                    <td>
                                        <table id="tblBinCategorical">

                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </div>

                    </fieldset>
                </fieldset>

            </div>
        </div>  %{--end container--}%

        %{-- ************************************************************************************************* --}%
        %{-- Tool Bar --}%
        %{-- ************************************************************************************************* --}%

        <fieldset class="toolFields">

            <div class="chkpair">
                <g:checkBox name="isBinning" onclick="lineGraphView.toggle_binning();"/> Enable binning
            </div>
            <div class="chkpair"><g:checkBox name="plotEvenlySpaced"/> Plot evenly spaced</div>

            <div>
                <label for="graphType">Graph type</label>
                <select id="graphType">
                    <option value="MERR">Mean with error bar</option>
                    <option value="MSTD">Mean with standard deviation</option>
                    <option value="MEDER">Median with error bar</option>
                    <option value="IND">Plot individuals</option>
                </select>
            </div>

            <input type="button" value="Run" onClick="lineGraphView.submit_job(this.form);" class="runAnalysisBtn">
        </fieldset>
    </form>
</div>
