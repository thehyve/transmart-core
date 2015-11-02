%{--include js lib for box plot dynamically--}%
<r:require modules="survival_analysis"/>
<r:layoutResources disposition="defer"/>

<div id="analysisWidget">

    %{--help and title--}%
    <h2>
        Variable Selection
        <a href='JavaScript:D2H_ShowHelp(1088,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
            <img src="${resource(dir: 'images', file: 'help/helpicon_white.jpg')}" alt="Help"/>
        </a>
    </h2>

    <form id="analysisForm">
        <div class="three-layout-container ">


            %{-- ************************************************************************************************* --}%
            %{-- Left inputs --}%
            %{-- ************************************************************************************************* --}%
            <div class="three-layout-left">
                <fieldset class="inputFields">
                    <h3>Time</h3>
                    <div class="divInputLabel">Select time variable from the Data Set Explorer Tree and drag it into
                    the box.  For example, "Survival Time".
                    This variable is required.</div>
                    <div id='divTimeVariable' class="queryGroupIncludeLong divInputBox"></div>
                    <div class="highDimBtns">
                        <button type="button" onclick="survivalAnalysisView.clear_high_dimensional_input('divTimeVariable')">Clear</button>
                    </div>
                </fieldset>
            </div>

            %{-- ************************************************************************************************* --}%
            %{-- Middle inputs --}%
            %{-- ************************************************************************************************* --}%
            <div class="three-layout-middle">
                <fieldset class="inputFields">
                    <div class="highDimContainer">
                        <h3>Category</h3>
                        <div class="divInputLabel">Select a variable on which you would like to sort the cohort and
                        drag it into the box. For example, "Cancer Stage".  If this variable is continuous (e.g. Age),
                        then it should be "binned" using the option below.
                        This variable is not required.</div>
                        <div id='divCategoryVariable' class="queryGroupIncludeLong divInputBox"></div>
                        <div class="highDimBtns">
                            <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divCategoryVariable', true)">High Dimensional Data</button>
                            <button type="button" onclick="survivalAnalysisView.clear_high_dimensional_input('divCategoryVariable')">Clear</button>
                        </div>
                        <input type="hidden" id="dependentVarDataType">
                        <input type="hidden" id="dependentPathway">
                    </div>

                    %{--Display independent variable--}%
                    <div id="displaydivCategoryVariable" class="independentVars"></div>

                    %{--Binning options--}%
                    <fieldset class="binningDiv">

                        <label for="variableType">Variable Type</label>
                        <select id="variableType" onChange="survivalAnalysisView.update_manual_binning();">
                            <option value="Continuous">Continuous</option>
                            <option value="Categorical">Categorical</option>
                        </select>

                        <label for="txtNumberOfBins">Number of Bins:</label>
                        <input type="text" id="txtNumberOfBins" onChange="survivalAnalysisView.manage_bins(this.value);" value="4" />

                        <label for="selBinDistribution">Bin Assignments</label>
                        <select id="selBinDistribution">
                            <option value="EDP">Evenly Distribute Population</option>
                            <option value="ESB">Evenly Spaced Bins</option>
                        </select>

                        <div class="chkpair">
                            <input type="checkbox" id="chkManualBin" onClick="survivalAnalysisView.manage_bins(document.getElementById('txtNumberOfBins').value);"> Manual Binning
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
            <div class="three-layout-right">
                <fieldset class="inputFields">
                    <h3>Censoring Variable</h3>
                    <div class="divInputLabel">
                        Drag the item for which to perform censoring in the analysis into this box. For example, when
                        performing Overall survival analysis, drag 'Survival status = alive' into this box.
                        This variable is not obligatory.
                    </div>
                    <div id='divCensoringVariable' class="queryGroupIncludeLong divInputBox"></div>
                    <div class="highDimBtns">
                        <button type="button" onclick="survivalAnalysisView.clear_high_dimensional_input('divCensoringVariable')">Clear</button>
                    </div>
                </fieldset>
            </div>

        </div>

        %{-- ************************************************************************************************* --}%
        %{-- Tool Bar --}%
        %{-- ************************************************************************************************* --}%
        <fieldset class="toolFields">
            <div class="chkpair">
                <g:checkBox name="isBinning" onclick="survivalAnalysisView.toggle_binning();"/> Enable binning
            </div>
            <input type="button" value="Run" onClick="survivalAnalysisView.submit_job(this.form);" class="runAnalysisBtn">
        </fieldset>
    </form>
</div>
