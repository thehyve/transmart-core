<g:each in="${aList}" status="i" var="a">
    <g:render template="/GWAS/bmanalysis" model="['analysis':a]" plugin="transmart-gwas"/>
</g:each>




