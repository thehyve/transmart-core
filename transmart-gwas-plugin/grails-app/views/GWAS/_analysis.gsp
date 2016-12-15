<g:each in="${aList}" status="i" var="a">
    <g:render template="/GWAS/bmanalysis" model="['analysis':a]"/>
</g:each>




