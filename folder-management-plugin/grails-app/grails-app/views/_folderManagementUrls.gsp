<script type="text/javascript" charset="utf-8">
    var exportAddURL = "${createLink([controller:'fileExport', action:'add'])}";
    var exportRemoveURL = "${createLink([controller:'fileExport', action:'remove'])}";
    var exportViewURL = "${createLink([controller:'fileExport', action:'view'])}";
    var exportURL = "${createLink([controller:'fileExport', action:'export'])}";

    var folderContentsURL = "${createLink([controller:'fmFolder',action:'getFolderContents'])}";
    var folderFilesURL = "${createLink([controller:'fmFolder',action:'getFolderFiles'])}";
    var folderDetailsURL = "${createLink(controller:'fmFolder',action:'folderDetail')}";
    var analysisDataURL = "${createLink([controller:'fmFolder',action:'analysisTable'])}";
    var editMetaDataURL = "${createLink([controller:'fmFolder',action:'editMetaData'])}";
    var createAnalysisURL = "${createLink([controller:'fmFolder',action:'createAnalysis'])}";
    var createAssayURL = "${createLink([controller:'fmFolder',action:'createAssay'])}";
    var createFolderURL = "${createLink([controller:'fmFolder',action:'createFolder'])}";
    var createStudyURL = "${createLink([controller:'fmFolder',action:'createStudy'])}";
    var createProgramURL = "${createLink([controller:'fmFolder',action:'createProgram'])}";
    var saveMetaDataURL = "${createLink([controller:'fmFolder',action:'updateMetaData'])}";
    var saveAssayURL = "${createLink([controller:'fmFolder',action:'saveAssay'])}";
    var saveAnalysisURL = "${createLink([controller:'fmFolder',action:'saveAnalysis'])}";
    var saveStudyURL = "${createLink([controller:'fmFolder',action:'saveStudy'])}";
    var saveFolderURL = "${createLink([controller:'fmFolder',action:'saveFolder'])}";
    var saveProgramURL = "${createLink([controller:'fmFolder',action:'saveProgram'])}";

    var deleteFileURL = "${createLink(controller:'fmFolder',action:'deleteFile')}";
</script>