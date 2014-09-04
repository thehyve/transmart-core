package fm

import grails.util.Holders

class FmFolderAssociation implements Serializable {

    static transients = ['bioObject']

    String objectUid
    String objectType
    FmFolder fmFolder

    static mapping = {
        table 'fm_folder_association'
        version false
        cache true
        sort "objectUid"
        id composite: ["objectUid", "fmFolder"]
        fmFolder column: 'folder_id'
    }

    static constraints = {

//		objectUid(unique: 'fmFolder')

    }

    static FmFolderAssociation get(String objectUid, long fmFolderId) {
        find 'from FmFolderAssociation where objectUid=:objectUid and fmFolder.id=:fmFolderId',
                [objectUid: objectUid, fmFolderId: fmFolderId]
    }

    static boolean remove(String objectUid, FmFolder fmFolder, boolean flush = false) {
        FmFolderAssociation instance = FmFolderAssociation.findByObjectUidAndFmFolder(objectUid, fmFolder)
        instance ? instance.delete(flush: flush) : false
    }

    public getBioObject() {
        log.info "ObjectUID=" + this.objectUid
        def bioData = org.transmart.biomart.BioData.findByUniqueId(this.objectUid)
        def clazz = lookupDomainClass()
        if (!clazz || !bioData) {
            return null
        } else {
//			return clazz.getObjectUid(this.objectUid)
            return clazz.get(bioData.id)
        }

    }

    protected Class lookupDomainClass() {
        //		def conf = SpringSecurityUtils.securityConfig
        // This probably should come from the config file

        String domainClassName = this.objectType //conf.rememberMe.persistentToken.domainClassName ?: ''
        def clazz = Holders.grailsApplication.getClassForName(domainClassName)
        if (!clazz) {
            log.error "Persistent token class not found: '${domainClassName}'"
        }

        return clazz
    }

    /**
     * override display
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("objectUid: ").append(this.objectUid).append(", objectType: ").append(this.objectType);
        sb.append(", Folder: ").append(this.fmFolder.id);
        return sb.toString();
    }


}
