%{--include js lib for heatmap dynamically--}%
<r:require modules="freq_plot"/>
<r:layoutResources disposition="defer"/>

%{--Input Container--}%
<div id="freq_plot_container" class="analysis_container"></div>

%{--Plot wrapper--}%
<div id="freq_plot_wrapper"></div>

%{-- template --}%
<div id="template-freq-plot" class="x-hidden">
    <div id="plotResultContainer" class="plot_result_container">
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
                <td>{regionVariable}</td>
            </tr>
            <tr>
                <td class="first-col">Group</td>
                <td>{inputGroupVariable}</td>
            </tr>
            <tr>
                <td class="first-col">Job Type</td>
                <td>{inputjobType}</td>
            </tr>
            </tbody>
        </table>
    </div>
</div>

