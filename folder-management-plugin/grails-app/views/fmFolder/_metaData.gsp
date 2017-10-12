<%@ page import="annotation.AmTagItem" %>

<asset:stylesheet href="uploadData.css"/>
<script type="text/javascript">$j = jQuery.noConflict();</script>
<asset:javascript src="uploadData.js" />


<div>
    <g:if test="${metaDataTagItems && metaDataTagItems.size()>0}">
        <g:each in="${metaDataTagItems as List<AmTagItem>}" status="i" var="amTagItem">
            <tr>
                <td valign="top" align="right" class="name">${amTagItem.displayName}&nbsp;<g:if test="${amTagItem.required == true}"><g:requiredIndicator/></g:if>:
                </td>
                <td valign="top" align="left" class="value">

                    <!-- FIXED -->
                    <g:if test="${amTagItem.tagItemType == 'FIXED'}">
                        <g:if test="${amTagItem.tagItemAttr!=null?bioDataObject?.hasProperty(amTagItem.tagItemAttr):false}" >
                            <g:if test="${amTagItem.tagItemSubtype == 'PICKLIST'}">
                                <g:select from="${fm.codes(amTagItem: amTagItem)}"
                                          name="${amTagItem.tagItemAttr}"  value="${fieldValue(bean:bioDataObject,field:amTagItem.tagItemAttr)}"
                                          optionKey="uniqueId" optionValue="codeName"  noSelection="['':'-Select One-']" />
                            </g:if>
                            <g:elseif test="${amTagItem.tagItemSubtype == 'MULTIPICKLIST'}">
                                <g:set var="metaDataService" bean="metaDataService"/>
                                <g:set var="fValue" value="${fieldValue(bean:bioDataObject,field:amTagItem.tagItemAttr)}"/>
                                <g:set var="displayValues" value="${fm.viewValues(fieldValue: fValue)}"/>
                                <g:render template="extTagSearchField" plugin="folderManagement"
                                          model="${[fieldName:amTagItem.tagItemAttr, codeTypeName:amTagItem.codeTypeName,
                                                    searchAction:'extSearch', searchController:'metaData', values:displayValues]}"/>

                            </g:elseif>
                            <g:elseif test="${amTagItem.tagItemSubtype == 'FREETEXT'}">
                                <g:if test="${fieldValue(bean:bioDataObject,field:amTagItem.tagItemAttr).length()<100}">
                                    <g:textField size="100" name="${amTagItem.tagItemAttr}"  value='${bioDataObject."${amTagItem.tagItemAttr}"?:""}'/>
                                </g:if>
                                <g:else>
                                    <g:textArea style="width: 100%" rows="10" name="${amTagItem.tagItemAttr}" value='${bioDataObject."${amTagItem.tagItemAttr}"?:""}' />
                                </g:else>
                            </g:elseif>
                            <g:elseif test="${amTagItem.tagItemSubtype == 'FREETEXTAREA'}">
                                <g:textArea style="width: 100%" rows="10" name="${amTagItem.tagItemAttr}" value='${bioDataObject."${amTagItem.tagItemAttr}"?:""}' />
                            </g:elseif>
                            <g:else>
                                ERROR -- Unrecognized tag item subtype
                            </g:else>
                        </g:if>
                    </g:if>
                    <g:elseif test="${amTagItem.tagItemType == 'CUSTOM'}">
                        <g:if test="${amTagItem.editable == false}">
                            not editable CUSTOM
                        </g:if>
                        <g:else>
                            <g:if test="${amTagItem.tagItemSubtype == 'FREETEXT'}">
                                <g:if test="${folder.uniqueId && amTagItem.id}">
                                    <g:set var="tagValue" value="${fm.tagValue(folder: folder, amTagItem: amTagItem)?.displayValue}"/>
                                </g:if>
                                <g:if test="${(tagValue ?: '')?.length() < 100}">
                                    <g:textField size="100" name="amTagItem_${amTagItem.id}"  value="${tagValue ?: ''}"/>
                                </g:if>
                                <g:else>
                                    <g:textArea size="100" cols="74" rows="10" name="amTagItem_${amTagItem.id}" value="${tagValue ?: ''}" />
                                </g:else>
                            </g:if>
                            <g:elseif test="${amTagItem.tagItemSubtype == 'FREETEXTAREA'}">
                                <g:if test="${folder.uniqueId && amTagItem.id}">
                                    <g:set var="tagValue" value="${fm.tagValue(folder: folder, amTagItem: amTagItem)?.displayValue}"/>
                                </g:if>
                                <g:textArea size="100" cols="74" rows="10" name="amTagItem_${amTagItem.id}" value="${tagValue?: ''}" />
                            </g:elseif>
                            <g:elseif test="${amTagItem.tagItemSubtype == 'PICKLIST'}">
                                <g:if test="${folder.uniqueId && amTagItem.id}">
                                    <g:set var="tagOptionUid" value="${fm.tagValue(folder: folder, amTagItem: amTagItem)?.objectUid}"/>
                                </g:if>
                                <g:select from="${fm.codes(amTagItem:  amTagItem)}"
                                          name="amTagItem_${amTagItem.id}" value="${tagOptionUid ?: ''}"
                                          optionKey="uniqueId" optionValue="codeName"  noSelection="['':'-Select One-']" />
                            </g:elseif>
                            <g:elseif test="${amTagItem.tagItemSubtype == 'MULTIPICKLIST'}">
                                <g:if test="${folder.uniqueId && amTagItem.id}">
                                    <g:set var="tagValues" value="${fm.tagValues(folder: folder, amTagItem: amTagItem)}"/>
                                </g:if>
                                <g:render template="extTagSearchField" plugin="folderManagement"
                                          model="${[fieldName:"amTagItem_"+amTagItem.id, codeTypeName:amTagItem.codeTypeName,
                                          searchAction:'extSearch', searchController:'metaData', values: tagValues]}"/>
                            </g:elseif>
                        </g:else>
                    </g:elseif>
                    <g:else>
                        <g:if test="${folder.uniqueId && amTagItem.id}">
                            <g:set var="tagValues" value="${fm.tagValues(folder: folder, amTagItem: amTagItem)}"/>
                        </g:if>
                        <g:if test="${amTagItem.tagItemSubtype == 'COMPOUNDPICKLIST'}">
                            <g:render template="${amTagItem.guiHandler}" plugin="folderManagement"
                                      model="${[measurements:measurements, technologies:technologies, vendors:vendors, platforms:platforms,
                                               fieldName:'amTagItem_' + amTagItem.id,searchAction:amTagItem.guiHandler + 'Search',
                                               searchController:'metaData', values:tagValues]}" />
                        </g:if>
                    <g:else>
                        <g:render template="extBusinessObjSearch" plugin="folderManagement"
                                  model="${[fieldName:'amTagItem_' + amTagItem.id,searchAction:amTagItem.guiHandler + 'Search',
                                           searchController:'metaData', values:tagValues]}" />
                    </g:else>
                </g:else>
            </td>
            </tr>
        </g:each>
    </g:if>
</div>

