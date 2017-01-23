<g:logMsg>uploadFiles.gsp started</g:logMsg>

<script type="text/javascript" src="${resource(dir:'js', file:'fine-uploader/iframe.xss.response-3.7.1.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js', file:'fine-uploader/jquery.fineuploader-3.7.1.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js', file:'fine-uploader/jquery.fineuploader-3.7.1.min.js')}"></script>
<link rel="stylesheet" type="text/css" href="${resource(dir:'css', file:'fineuploader-3.5.0.css')}">

<g:logMsg>parentFolder '${parentFolder}'</g:logMsg>
<g:logMsg>parentFolderName '${parentFolder?.folderName}'</g:logMsg>
<g:logMsg>parentFolderId '${parentFolder?.id}'</g:logMsg>

<div id="uploadtitle"><p>Upload files in folder ${parentFolder?.folderName}</p></div>
<div id="fine-uploader-basic" class="btn btn-success">
  <i class="icon-upload icon-white"></i> <p>To upload files, click or drag files in this zone.</p>
</div>

<table style="width: 100%;" class="uploadtable" id="uploadtable">
</table>

 <form name="form">
    <input type="hidden" name="parent" id="parentFolderId" value="${parentFolder?.id}" />
    <input type="hidden" name="parentName" id="parentFolderName" value="${parentFolder?.folderName}" />
    <input type="hidden" name="existingfiles" id="existingfiles" value="yes" />
</form>

<g:logMsg>_uploadFiles.gsp done</g:logMsg>
