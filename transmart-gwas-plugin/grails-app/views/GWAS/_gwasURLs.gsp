<script type="text/javascript" charset="utf-8">
    var regionBrowseWindow = "${createLink([controller:'gwasSearch',action:'getRegionFilter'])}";
    var eqtlTranscriptGeneWindow = "${createLink([controller:'gwasSearch',action:'getEqtlTranscriptGeneFilter'])}";
    var getQQPlotURL = "${createLink([controller:'gwasSearch',action:'getQQPlotImage'])}";
    var webStartURL = "${createLink([controller:'gwasSearch',action:'webStartPlotter'])}";
    var getTableDataURL = "${createLink([controller:'gwasSearch',action:'getTableResults'])}";
    var getAnalysisDataURL = "${createLink([controller:'gwasSearch',action:'getAnalysisResults'])}";
    var exportAnalysisURL = "${createLink([controller:'gwasSearch', action:'exportAnalysis'])}";
</script>