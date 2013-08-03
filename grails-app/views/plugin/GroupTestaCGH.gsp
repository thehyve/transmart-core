%{--Input Container--}%
<div id="gtContainer"></div>

%{-- Analysis Output --}%
<div id="intermediateResultWrapper"></div>

%{--Plot wrapper--}%
<div id="gtPlotWrapper"></div>

%{-- template --}%
<div id="template-group-test-plot" class="x-hidden">
	<div id="plotResultContainer" class="plotResultContainer">
		%{--Image--}%
		<g:img file="{filename}" class="freq-plot"></g:img>
		<hr class="separator"/>
		%{--Download button --}%
		<div class="resultToolBar">
			<div id="downloadBtn"></div>
		</div>
        <table class="newspaper-a">
            <caption>Table 1: Input Parameters </caption>
            <thead>
            <tr>
                <th scope="col" class="first-col">Parameters</th>
                <th scope="col">Value</th>
            </tr>
            </thead>

            <tbody>
            <tr>
                <td class="first-col">Job Name</td>
                <td>{jobName}</td>
            </tr>
            <tr>
                <td class="first-col">Started date</td>
                <td>{startDate}</td>
            </tr>
            <tr>
                <td class="first-col">Run Time</td>
                <td>{runTime}</td>
            </tr>
            <tr>
                <td class="first-col">Selected Cohort</td>
                <td>
                    <div> Subset 1: {inputCohort1} </div>
                    <div> Subset 2: {inputCohort2} </div>
                </td>
            </tr>
            <tr>
                <td class="first-col">Region</td>
                <td>{inputRegion}</td>
            </tr>
            <tr>
                <td class="first-col">Group</td>
                <td>{inputGroupVariable}</td>
            </tr>
            <tr>
                <td class="first-col">Statistical Test</td>
                <td>{inputStatisticsType}</td>
            </tr>
            <tr>
                <td class="first-col">Alteration Type</td>
                <td>{inputAlteration}</td>
            </tr>
            </tbody>
        </table>
	</div>
</div>
