<%--
  User: riza
  Date: 23-04-13
  Time: 11:33
--%>

%{--Input Container--}%
<div id="gtContainer"></div>

%{--Plot wrapper--}%
<div id="gtPlotWrapper"></div>

%{-- template --}%
<extjs-tpl id="template-survival-plot" class="x-hidden">
	<div id="plotResultContainer" class="plotResultContainer">
		<h1>Frequency Plot</h1>

		<div id="plotBody" class="plotBody">
			<div id="plotCurve">
				<g:img dir="images/tempImages/{foldername}" file="{filename}" height='350' width='350'></g:img>
			</div>
		</div>

	</div>
</extjs-tpl>