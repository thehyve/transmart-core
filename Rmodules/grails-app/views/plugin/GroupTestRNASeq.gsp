<html>
<body>
<asset:javascript src="r-modules.js"/>

%{--Input Container--}%
<div id="rgtContainer"></div>

%{-- Analysis Output --}%
<div id="intermediateResultWrapper"></div>

%{--Plot wrapper--}%
<div id="rgtPlotWrapper"></div>

%{-- template --}%
<div id="template-group-test-rnaseq-plot" class="x-hidden">
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
                <td class="first-col">RNASeq</td>
                <td>{inputRNASeqVariable}</td>
            </tr>
            <tr>
                <td class="first-col">Group</td>
                <td>{inputGroupVariable}</td>
            </tr>
            <tr>
                <td class="first-col">Analysis Type</td>
                <td>{inputAnalysisType}</td>
            </tr>
            </tbody>
        </table>
	</div>
</div>
</body>
</html>
