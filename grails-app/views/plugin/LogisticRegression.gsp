%{--include js lib for box plot dynamically--}%
<r:require modules="logistic_regression"/>
<r:layoutResources disposition="defer"/>


<div id="analysisWidget">

    %{--help and title--}%
    <h2>
        Variable Selection
    </h2>

    <form id="analysisForm">
        <div class="container">

            %{-- ************************************************************************************************* --}%
            %{-- Left inputs --}%
            %{-- ************************************************************************************************* --}%
            <div class="left">
                <fieldset class="inputFields">
                    <h3>Independent Variable</h3>
                    <span class="hd-notes">
                        Drag a <b>numerical</b> concept from the tree into the box below. The concept must come from a
                        data node (Biomarker Data or Clinical Data).  <br><br><br><br><br><br><br>
                    </span>
                    <div id='divIndependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                    <div class="highDimBtns">
                        <button type="button" onclick="logisticRegressionView.clear_high_dimensional_input('divIndependentVariable')">Clear</button>
                    </div>


                </fieldset>
            </div>

            %{-- ************************************************************************************************* --}%
            %{-- Right inputs --}%
            %{-- ************************************************************************************************* --}%

            <div class="right">
                <fieldset class="inputFields">
                    <h3>Outcome</h3>
                    <span class="hd-notes">
                        For the binary response, among two possible outcomes (Commonly the generic terms success and
                        failure are used for these two outcomes). Drag the two related <b>categorical concepts</b>
                        from the tree into the box below (for example, Subjects with Malignant Vs. Subjects with
                        Benign Tumors). A folder may be dragged in to include the two leaf nodes under that folder.
                        <br> <i>NOTE: The top concept will always be designated a value of 1 and the other a
                        value of 0.</i>
                    </span>
                    <div id='divGroupByVariable' class="queryGroupIncludeSmall highDimBox"></div>
                    <div class="highDimBtns">
                        <button type="button" onclick="logisticRegressionView.clear_high_dimensional_input('divGroupByVariable')">Clear</button>
                    </div>

                    %{--Binning options--}%
                    <fieldset class="binningDiv">

                        <label for="variableType">Variable Type</label>
                        <select id="variableType" onChange="logisticRegressionView.update_manual_binning();">
                            <option value="Continuous">Continuous</option>
                            <option value="Categorical">Categorical</option>
                        </select>

                        <label for="txtNumberOfBins">Number of Bins:</label>
                        <input type="text" id="txtNumberOfBins" onChange="logisticRegressionView.manage_bins(this.value);" value="2" disabled />

                        <label for="selBinDistribution">Bin Assignments</label>
                        <select id="selBinDistribution">
                            <option value="EDP">Evenly Distribute Population</option>
                            <option value="ESB">Evenly Spaced Bins</option>
                        </select>

                        <div class="chkpair">
                            <input type="checkbox" id="chkManualBin" onClick="logisticRegressionView.manage_bins(document.getElementById('txtNumberOfBins').value);"> Manual Binning
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
                <g:checkBox name="isBinning" onclick="logisticRegressionView.toggle_binning();"/> Enable binning
            </div>

            <input type="button" value="Run" onClick="logisticRegressionView.submit_job(this.form);" class="runAnalysisBtn">
        </fieldset>
    </form>
</div>
