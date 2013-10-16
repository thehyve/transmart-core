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
                        text : 'Download all files', handler : function()
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

FM.addFileNodes = function(response, node) {
    var json = Ext.util.JSON.decode(response.responseText);
    node.beginUpdate();
    for(var fileId in json){
        node.appendChild(FM.getFileNode(fileId, json[fileId]));
    }
    node.endUpdate();
}