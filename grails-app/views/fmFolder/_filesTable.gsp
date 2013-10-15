<div style="margin-left: 25px">
    <table class="analysis-table filelisttable" style="table-layout: fixed" id="file-list-table-${folder.id}" name="${folder.id}">
        <colgroup>
            <col span="1" class="wide"/>
            <col span="3" />
        </colgroup>
        <thead>
        <tr>
            <th class="columnheader">File Name</th>
            <th class="columnheader">Created on</th>
            <th class="columnheader">Updated on</th>
            <th class="columnheader">&nbsp;</th>
        </tr>
        </thead>
        <tfoot>
        <tr>
            <td colspan="3">&nbsp;</td>
            <td>
                <div style="padding: 4px 0px;">
                    <span class="greybutton addall link">Export all</span>
                </div>
            </td>
        </tr>
        </tfoot>
        <tbody>
        <g:each in="${folder?.fmFiles.sort{a,b-> a.displayName.toUpperCase().compareTo(b.displayName.toUpperCase())}}" status="i" var="fmFile">
            <tr id="${fmFile.id}-filerow" class="" style="border-top: 1px solid #ccc">
                <td class="columnname" style="text-align: left;"><span class="fileicon ${fmFile.fileType}"></span>&nbsp;${fmFile.displayName}</td>
                <td class="columnvalue">
                    <g:formatDate format="yyyy-MM-dd" date="${fmFile.createDate}" />
                </td>
                <td class="columnvalue">
                    <g:formatDate format="yyyy-MM-dd" date="${fmFile.updateDate}" />
                </td>
                <td class="columnvalue">
                    <div>
                        <span class="exportaddspan foldericon addcart link" name="${fmFile.id}">&nbsp;</span>
                        <sec:ifAnyGranted roles="ROLE_ADMIN">
                            <span class="deletefilespan foldericon deletefile link" name="${fmFile.id}">&nbsp;</span>
                        </sec:ifAnyGranted>
                    </div>
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>
</div>