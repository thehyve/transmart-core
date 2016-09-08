/**
 * Created with IntelliJ IDEA.
 * User: davinewton
 * Date: 10/16/13
 * Time: 4:03 PM
 */
FM = []

FM.getFileTreeSorter = function(ontTree) {
    var ontTreeSorter = new Ext.tree.TreeSorter(ontTree,
        {
            folderSort : true
        }
    );

    var dsc = ontTreeSorter.dir && ontTreeSorter.dir.toLowerCase() == "desc";
    var p = ontTreeSorter.property || "text";
    var sortType = ontTreeSorter.sortType;
    var fs = ontTreeSorter.folderSort;
    var cs = ontTreeSorter.caseSensitive === true;
    var leafAttr = ontTreeSorter.leafAttr || 'leaf';

    ontTreeSorter.sortFn = function(n1, n2){
        if(n1.attributes.cls == 'fileFolderNode') {
            return -1;
        }
        if(n2.attributes.cls == 'fileFolderNode') {
            return 1;
        }
        if(fs){
            if(n1.attributes[leafAttr] && !n2.attributes[leafAttr]){
                return 1;
            }
            if(!n1.attributes[leafAttr] && n2.attributes[leafAttr]){
                return -1;
            }
        }
        var v1 = sortType ? sortType(n1) : (cs ? n1.attributes[p] : n1.attributes[p].toUpperCase());
        var v2 = sortType ? sortType(n2) : (cs ? n2.attributes[p] : n2.attributes[p].toUpperCase());
        if(v1 < v2){
            return dsc ? +1 : -1;
        }else if(v1 > v2){
            return dsc ? -1 : +1;
        }else{
            return 0;
        }
    };

    return ontTreeSorter;
}

FM.getFileFolderNode = function(parentNode) {
    var key = parentNode.attributes.id;
    var newnode = new Ext.tree.AsyncTreeNode({
        text: "Files",
        draggable: false,
        leaf: false,
        id: key + "\\FILES",
        comment: "Files for " + key,
        qtip: "Files for " + key,
        iconCls: 'fileFolderNode',
        cls: 'fileFolderNode',
        level: 0,  //extra attribute for storing level in hierarchy access through node.attributes.level
        expanded: false,
        accession: parentNode.attributes.accession
    });
    newnode.addListener('contextmenu',FM.fileFolderRightClick);

    return newnode;
}

FM.getFileNode = function(fileId, fileParams) {

    var displayName = fileParams.displayName;
    var fileType = fileParams.fileType;

    var newnode = new Ext.tree.AsyncTreeNode({
        text: displayName,
        draggable: false,
        leaf: true,
        id: "FILE:" + fileId,
        iconCls: 'fileicon ' + fileType,
        cls: 'fileNode ' + fileType,
        level: 0,  //extra attribute for storing level in hierarchy access through node.attributes.level
        expanded: false,
        fileId: fileId
    });
    newnode.addListener('contextmenu',FM.fileNodeRightClick);

    return newnode;
}

FM.fileFolderRightClick = function(eventNode, event)
{
    if (!this.contextMenuFileFolder)
    {
        this.contextMenuFileFolder = new Ext.menu.Menu(
            {
                id : 'contextMenuFileFolder',
                items : [
                    {
                        text : 'Download all files in this study', handler : function()
                    {
                        window.location.href = pageInfo.basePath + "/fileExport/exportStudyFiles/?accession=" + eventNode.attributes.accession;
                    }
                    }
                ]
            }
        );
    }
    var xy = event.getXY();
    this.contextMenuFileFolder.showAt(xy);
    return false;
}

FM.fileNodeRightClick = function(eventNode, event)
{
    if (!this.contextMenuFile)
    {
        this.contextMenuFile = new Ext.menu.Menu(
            {
                id : 'contextMenuFile',
                items : [
                    {
                        text : 'Show details', handler : function()
                    {
                        FM.openFileDetailsWindow(eventNode);
                    }
                    }
                    ,
                    {
                        text : 'Open', handler : function()
                    {
                        window.open(pageInfo.basePath + "/fileExport/exportFile/?id=" + eventNode.attributes.fileId + "&open=true", '_blank');
                    }
                    }
                    ,
                    {
                        text : 'Download', handler : function()
                    {
                        window.location.href = pageInfo.basePath + "/fileExport/exportFile/?id=" + eventNode.attributes.fileId;
                    }
                    }
                ]
            }
        );
    }
    var xy = event.getXY();
    this.contextMenuFile.showAt(xy);
    return false;
}

FM.handleFolderFilesRequest = function(source, node, callback) {
    return Ext.Ajax.request({
        url: pageInfo.basePath+"/fmFolder/getFolderFiles",
        method: 'GET',
        success: source.handleResponse,
        failure: source.handleFailure,
        scope: source,
        argument: {callback: callback, node: node},
        timeout: '120000', //2 minutes
        params: {returnJSON: true, accession: node.attributes.accession}
    });
}

FM.handleFolderHasFilesRequest = function(source, originalResponse, node, callback) {

    Ext.Ajax.request({
        url: pageInfo.basePath+"/fmFolder/getFolderHasFiles",
        method: 'GET',
        success: function(response) {
            if (response.responseText == "true") {
                node.appendChild(FM.getFileFolderNode(node));
            }
            source.parseXml(originalResponse, node);
            source.endAppending(node, callback);
        },
        timeout: '120000', //2 minutes
        params: {accession: node.attributes.accession}
    });


}

FM.addFileNodes = function(source, response, node, callback) {
    var json = Ext.util.JSON.decode(response.responseText);
    node.beginUpdate();
    for(var fileId in json){
        node.appendChild(FM.getFileNode(fileId, json[fileId]));
    }
    source.endAppending(node, callback);
}

FM.openFileDetailsWindow = function(node)
{

    if( !FM.filedetailswin)
    {
        FM.filedetailswin = new Ext.Window(
            {
                id : 'showFileDetailsWindow',
                title : 'File Details',
                layout : 'fit',
                width : 600,
                height : 500,
                closable : false,
                plain : true,
                modal : true,
                border : false,
                autoScroll: true,
                buttons : [
                    {
                        text : 'Close',
                        handler : function()
                        {
                            FM.filedetailswin.hide();
                        }
                    }
                ],
                resizable : false
            }
        );
    }

    FM.filedetailswin.show(viewport);
    FM.filedetailswin.header.update("File Details - " + node.attributes.text);

    FM.filedetailswin.load({
        url: pageInfo.basePath+"/fmFile/show",
        params: {id:node.attributes.fileId},
        discardUrl: true,
        nocache: true,
        text: "Loading...",
        timeout: 30000,
        scripts: false
    });
    //}

}