%{--include js lib for table fisher dynamically--}%
<r:require modules="table_fisher"/>
<r:layoutResources disposition="defer"/>

<div id="analysisWidget">

    %{--help and title--}%
    <h2>
        Variable Selection
        <a href='JavaScript:D2H_ShowHelp(1505,helpURL,"wndExternal",CTXT_DISPLAY_FULLHELP )'>
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
                        <span class="hd-notes">Select two or more variables from the Data Set Explorer Tree and drag
                        them into the box. A continuous variable may be selected and categorized by enabling the
                        "Binning" below.</span>
                        <div id='divIndependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                        <div class="highDimBtns">
                            <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divIndependentVariable', true)">High Dimensional Data</button>
                            <button type="button" onclick="tableWithFisherView.clear_high_dimensional_input('divIndependentVariable')">Clear</button>
                        </div>
                        <input type="hidden" id="independentVarDataType">
                        <input type="hidden" id="independentPathway">
                    </div>

                    %{--Display independent variable--}%
                    <div id="displaydivIndependentVariable" class="independentVars"></div>

                    %{--Binning for independent variable--}%
                    <fieldset class="binningDiv">

                        <div class="chkpair">
                            <input type="checkbox" id="EnableBinningIndep">
                            Bin the Independent Variable
                        </div>

                        <label for="variableTypeIndep">Variable Type</label>
                        <select id="variableTypeIndep" onChange="tableWithFisherView.update_manual_binning_fisher('Indep');">
                            <option value="Continuous">Continuous</option>
                            <option value="Categorical">Categorical</option>
                        </select>

                        <label for="txtNumberOfBinsIndep">Number of Bins:</label>
                        <input type="text" id="txtNumberOfBinsIndep" onChange="tableWithFisherView.manage_bins_fisher(this.value,'Indep');" value="2" />

                        <label for="selBinDistributionIndep">Bin Assignments</label>
                        <select id="selBinDistributionIndep">
                            <option value="EDP">Evenly Distribute Population</option>
                            <option value="ESB">Evenly Spaced Bins</option>
                        </select>

                        <div class="chkpair">
                            <input type="checkbox" id="chkManualBinIndep" onClick="tableWithFisherView.manage_bins_fisher(document.getElementById('txtNumberOfBinsIndep').value,'Indep');"> Manual Binning
                        </div>

                        %{-- Manual binning for independent variable --}%
                        <div id="divManualBinContinuousIndep" style="display: none;">
                            <table id="tblBinContinuousIndep">
                                <tr>
                                    <td>Bin Name</td>
                                    <td colspan="2">Range</td>
                                </tr>
                            </table>
                        </div>

                        <div id="divManualBinCategoricalIndep" style="display: none;">
                            <table>
                                <tr>
                                    <td style="vertical-align: top;">Categories
                                        <div id='divCategoricalItemsIndep' class="manualBinningCategories"></div>
                                    </td>
                                    <td style="vertical-align: top;"><br />
                                        <br><span class="minifont">&laquo;Drag To Bin&raquo</span></td>
                                    <td>
                                        <table id="tblBinCategoricalIndep">

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
                    %{--Dependent variable--}%
                    <div class="highDimContainer">
                        <h3>Dependent Variable</h3>
                        <span class="hd-notes">Select two or more variables from the Data Set Explorer Tree and drag them into the box.
                        A continuous variable may be selected and categorized by enabling the "Binning" below.</span>
                        <div id='divDependentVariable' class="queryGroupIncludeSmall highDimBox"></div>
                        <div class="highDimBtns">
                            <button type="button" onclick="highDimensionalData.gather_high_dimensional_data('divDependentVariable', true)">High Dimensional Data</button>
                            <button type="button" onclick="tableWithFisherView.clear_high_dimensional_input('divDependentVariable')">Clear</button>
                        </div>
                        <input type="hidden" id="dependentVarDataType">
                        <input type="hidden" id="dependentPathway">
                    </div>

                    %{--Display dependent variable--}%
                    <div id="displaydivDependentVariable" class="dependentVars"></div>

                    %{--Binning for dependent variable--}%
                    <fieldset class="binningDiv">

                        <div class="chkpair">
                            <input type="checkbox" id="EnableBinningDep">
                            Bin the Dependent Variable
                        </div>

                        <label for="variableTypeDep">Variable Type</label>
                        <select id="variableTypeDep" onChange="tableWithFisherView.update_manual_binning_fisher('Dep');">
                            <option value="Continuous">Continuous</option>
                            <option value="Categorical">Categorical</option>
                        </select>

                        <label for="txtNumberOfBinsDep">Number of Bins:</label>
                        <input type="text" id="txtNumberOfBinsDep" onChange="tableWithFisherView.manage_bins_fisher(this.value,'Dep');" value="2" />

                        <label for="selBinDistributionDep">Bin Assignments</label>
                        <select id="selBinDistributionDep">
                            <option value="EDP">Evenly Distribute Population</option>
                            <option value="ESB">Evenly Spaced Bins</option>
                        </select>

                        <div class="chkpair">
                            <input type="checkbox" id="chkManualBinDep" onClick="tableWithFisherView.manage_bins_fisher(document.getElementById('txtNumberOfBinsDep').value,'Dep');"> Manual Binning
                        </div>

                        %{-- Manual binning for Dependent variable --}%
                        <div id="divManualBinContinuousDep" style="display: none;">
                            <table id="tblBinContinuousDep">
                                <tr>
                                    <td>Bin Name</td>
                                    <td colspan="2">Range</td>
                                </tr>
                            </table>
                        </div>

                        <div id="divManualBinCategoricalDep" style="display: none;">
                            <table>
                                <tr>
                                    <td style="vertical-align: top;">Categories
                                        <div id='divCategoricalItemsDep' class="manualBinningCategories"></div>
                                    </td>
                                    <td style="vertical-align: top;"><br />
                                        <br /><span class="minifont">&laquo;Drag To Bin&raquo;</span></td>
                                    <td>
                                        <table id="tblBinCategoricalDep">

                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </div>

                    </fieldset>
                </fieldset>
            </div>

        </div> %{--end container--}%



        %{-- ************************************************************************************************* --}%
        %{-- Tool Bar --}%
        %{-- ************************************************************************************************* --}%
        <fieldset class="toolFields">
            <div class="chkpair">
                <g:checkBox name="isBinning" onclick="tableWithFisherView.toggle_binning_fisher();"/> Enable binning
            </div>
            <input type="button" value="Run" onClick="tableWithFisherView.submit_job(this.form);" class="runAnalysisBtn">
        </fieldset>

    </form>
</div>