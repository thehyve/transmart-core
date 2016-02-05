<div style="margin-left: 25px">
    <table class="details-table filelisttable" id="file-list-table-${folder.id}" name="${folder.id}">
        <thead>
            <tr>
                <th class="columnheader">File Name</th>
                <th class="columnheader" style="width: 70px">Size</th>
                <th class="columnheader" style="width: 70px">Created on</th>
                <th class="columnheader" style="width: 70px">Updated on</th>
                <th class="columnheader" style="width: 160px">&nbsp;</th>
            </tr>
        </thead>
        <tfoot>
            <tr>
                <td colspan="3">&nbsp;</td>
                <td style="padding: 4px 0px;">
                        <span class="foldericon addall link">Export all</span>
                    </div>
                </td>
            </tr>
        </tfoot>
        <tbody>
            <g:each in="${folder?.fmFiles.sort{a,b-> a.displayName.toUpperCase().compareTo(b.displayName.toUpperCase())}}" status="i" var="fmFile">
	        <g:logMsg>fmfile '${fmFile.displayName}' id ${fmFile.id} type ${fmFile.fileType} originalName '${fmFile.originalName}' cre ${fmFile.createDate} upd ${fmFile.updateDate}</g:logMsg>
                <tr class="details-row ${(i % 2) == 0 ? 'odd' : 'even'}" id="${fmFile.id}-filerow">
                    <td class="columnname textsmall" style="text-align: left;">
                        <span class="fileicon ${fmFile.fileType}"></span>
		        <g:link controller="fmFolder" action="downloadFile" params="[id: fmFile.id]">
                           <g:if test="${hlFileIds?.contains(fmFile.uniqueId)}">
                               <mark><b>${fmFile.displayName}</b></mark>
                           </g:if>
                           <g:else>
                               ${fmFile.displayName}
                           </g:else>
                        </g:link>
		    </td>
                    <td class="columnvalue textsmall" style="text-align: right;">
                        ${fmFile.fileSize}
                    </td>
                    <td class="columnvalue textsmall">
                        <g:formatDate format="yyyy-MM-dd" date="${fmFile.createDate}"/>
                    </td>
                    <td class="columnvalue textsmall">
                        <g:formatDate format="yyyy-MM-dd" date="${fmFile.updateDate}"/>
                    </td>
                    <td class="columnvalue textsmall">
                        <div>
                            <span class="exportaddspan foldericon addcart link" name="${fmFile.id}">Add to export</span>
                            <sec:ifAnyGranted roles="ROLE_ADMIN">
                                <span class="deletefilespan foldericon deletefile link" name="${fmFile.id}"> Delete</span>
                            </sec:ifAnyGranted>
                        </div>
                    </td>
                </tr>
            </g:each>
        </tbody>
    </table>
</div>
