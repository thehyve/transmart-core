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
                            <button type="button" onclick="boxPlotView.clear_high_dimensional_input('divGroupByVariable')">Clear</button>
                        </div>
                        <input type="hidden" id="independentVarDataType">
                        <input type="hidden" id="independentPathway">
                    </div>

                    %{--Display independent variable--}%
                    <div id="displaydivDependentVariable" class="dependentVars"></div>

                    %{--Binning options--}%
                    <fieldset class="binningDiv">

                        <label for="selBinVariableSelection">Variable:</label>
                        <select id="selBinVariableSelection">
                            <option value="IND">Independent</option>
                            <option value="DEP">Dependent</option>
                        </select>

                        <label for="variableType">Variable Type</label>
                        <select id="variableType" onChange="boxPlotView.update_manual_binning();">
                            <option value="Continuous">Continuous</option>
                            <option value="Categorical">Categorical</option>
                        </select>

                        <label for="txtNumberOfBins">Number of Bins:</label>
                        <input type="text" id="txtNumberOfBins" onChange="boxPlotView.manage_bins(this.value);" value="4" />

                        <label for="selBinDistribution">Bin Assignments</label>
                        <select id="selBinDistribution">
                            <option value="EDP">Evenly Distribute Population</option>
                            <option value="ESB">Evenly Spaced Bins</option>
                        </select>

                        <div class="chkpair">
                            <input type="checkbox" id="chkManualBin" onClick="boxPlotView.manage_bins(document.getElementById('txtNumberOfBins').value);"> Manual Binning
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
                            <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divGroupByVariable')">High Dimensional Data</button>
                            <button type="button" onclick="boxPlotView.clear_high_dimensional_input('divGroupByVariable')">Clear</button>
                        </div>
                        <input type="hidden" id="dependentVarDataType">
                        <input type="hidden" id="dependentPathway">
                    </div>

                    %{--Display dependent variable--}%
                    <div id="displaydivGroupByVariable" class="dependentVars"></div>
                </fieldset>

            </div>

        </div>  %{--end container--}%

    %{-- ************************************************************************************************* --}%
    %{-- Tool Bar --}%
    %{-- ************************************************************************************************* --}%
        <fieldset class="toolFields">
            <div class="chkpair">
                <g:checkBox name="isBinning" onclick="boxPlotView.toggle_binning();"/> Enable binning
            </div>
            <input type="button" value="Run" onClick="boxPlotView.submit_job(this.form);" class="runAnalysisBtn">
        </fieldset>
    </form>

</div>
