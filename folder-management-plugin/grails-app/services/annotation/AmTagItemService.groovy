package annotation

import fm.FmFolder
import grails.transaction.Transactional

@Transactional
class AmTagItemService {

    def serviceMethod() {

    }

    def getDisplayItems(Long key) {

        log.info "getDisplayItems Searching amTagItems for tag template " + key

        def amTagItems

        if (key) {
            Map<String, Object> paramMap = new HashMap<Long, Object>();

            StringBuffer sb = new StringBuffer();
            sb.append("from AmTagItem ati where viewInGrid=1 ");
            sb.append(" and ati.amTagTemplate.id = :amTagTemplateId order by displayOrder");
            paramMap.put("amTagTemplateId", key);

            amTagItems = AmTagItem.findAll(sb.toString(), paramMap);

            log.info "amTagItems = " + amTagItems + " for key = " + key
        } else {
            log.error "Unable to retrieve an amTagItems with a null key value"
        }


        return amTagItems
    }

    def getChildDisplayItems(Long key) {
        log.info "getChildDisplayItems Searching child amTagItems for tag template " + key

        def amTagItems

        if (key) {
            Map<String, Object> paramMap = new HashMap<Long, Object>();

            StringBuffer sb = new StringBuffer();
            sb.append("from AmTagItem ati where viewInChildGrid=1 ");
            sb.append(" and ati.amTagTemplate.id = :amTagTemplateId order by displayOrder");
            paramMap.put("amTagTemplateId", key);

            amTagItems = AmTagItem.findAll(sb.toString(), paramMap);

            log.info "amTagItems = " + amTagItems + " for key = " + key
        } else {
            log.error "Unable to retrieve an child amTagItems with a null key value"
        }

        return amTagItems

    }

    def getEditableItems(Long key) {
        log.info "getEditableItems Searching amTagItems for tag template " + key

        def amTagItems = []

        if (key) {
            amTagItems = AmTagItem.findAll(
                "from AmTagItem ati where ati.amTagTemplate.id = :templateId and ati.editable = '1' order by ati.displayOrder",
                [templateId: key])

            log.info "amTagItems = ${amTagItems} for key = ${key}"
        } else {
            log.error "Unable to retrieve an amTagItems with a null key value"
        }

        amTagItems
    }

    def getRequiredItems(Long key) {

        log.info "getRequiredItems Searching amTagItems for tag template " + key

        def amTagItems

        if (key) {
            Map<String, Object> paramMap = new HashMap<Long, Object>();

            StringBuffer sb = new StringBuffer();
            sb.append("from AmTagItem ati where required=1 ");
            sb.append(" and ati.amTagTemplate.id = :amTagTemplateId order by displayOrder");
            paramMap.put("amTagTemplateId", key);

            amTagItems = AmTagItem.findAll(sb.toString(), paramMap);

            log.info "amTagItems = " + amTagItems + " for key = " + key
        } else {
            log.error "Unable to retrieve an amTagItems with a null key value"
        }


        return amTagItems
    }

    def beforeValidate(FmFolder folder, params) {
        if (folder.folderName) {
            def amTagTemplate = AmTagTemplate.findByTagTemplateType(folder.folderName)
            def metaDataTagItems = amTagItemService.getRequiredItems(amTagTemplate.id)
            metaDataTagItems.each
                    {
                        if (it.tagItemType != 'FIXED') {
                            if (null != params."amTagItem_${it.id}" && "" != params."amTagItem_${it.id}") {
                                folder.errors.addError(it.displayName, it.displayName + " is required")
                            }
                        }
                    }
        } else {
            folder.errors.addError("folderName", "Folder name must have a value")
        }

    }

}
