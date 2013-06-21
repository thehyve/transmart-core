%{--Input Container--}%
<div id="gtContainer"></div>

%{-- Analysis Output --}%
<div id="intermediateResultWrapper"></div>

%{--Plot wrapper--}%
<div id="gtPlotWrapper"></div>

%{-- template --}%
<extjs-tpl id="template-group-test-plot" class="x-hidden">
	<div id="plotResultContainer" class="plotResultContainer">
		%{--Image--}%
		<g:img file="{filename}" class="freq-plot"></g:img>
		<hr class="separator"/>
		%{--Download button --}%
		<div class="resultToolBar">
			<div id="downloadBtn"></div>
		</div>
	</div>
</extjs-tpl>
