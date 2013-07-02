%{--Survival Analysis aCGH input Container--}%
<div id="analysisContainer"></div>

%{-- Analysis Output --}%
<div id="intermediateResultWrapper"></div>

%{--Plot wrapper--}%
<div id="plotResultWrapper"></div>

%{--==========================--}%
%{--Template for Survival Plot--}%
%{--==========================--}%

<div id="template-survival-plot" class="x-hidden x-hide-display">
	<div id="plotResultContainer" class="plotResultContainer">
		<h1>Chr: {chromosome} ({start}-{end})</h1>

		<div id="plotBody" class="plotBody">
			<div id="plotCurve">
				<g:img file="{filename}"  class="survivalPlotImg"></g:img>
			</div>

			<div id="plotCurveDesc">
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
						<td class="first-col">Survival Time</td>
						<td>{inputSurvivalTime}</td>
					</tr>
					<tr>
						<td class="first-col">Censoring Variable</td>
						<td>{inputCensoring}</td>
					</tr>
					<tr>
						<td class="first-col">Alteration Type</td>
						<td>{inputAlteration}</td>
					</tr>
					</tbody>
				</table>
				<br/>
				<table class="newspaper-a">
					<caption>Table 2: Result</caption>
					<thead>
					<tr>
						<th scope="col" class="first-col">Chromosome</th>
						<th scope="col">cytoband</th>
						<th scope="col">start</th>
						<th scope="col">end</th>
						<th scope="col">p-value</th>
						<th scope="col">fdr</th>
					</tr>
					</thead>
					<tbody>
					<tr>
						<td class="first-col">{chromosome}</td>
						<td>{cytoband}</td>
						<td>{start}</td>
						<td>{end}</td>
						<td>{pvalue}</td>
						<td>{fdr}</td>
					</tr>
					</tbody>
				</table>
			</div>
		</div>
	</div>
</div>