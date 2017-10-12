package annotation

import grails.transaction.Transactional
import org.transmart.biomart.BioData
import org.transmart.biomart.ConceptCode
import org.transmartproject.browse.fm.FmFolder

@Transactional
class MetaDataService {

    List<AmTagDisplayValue> getTagValues(FmFolder folder, AmTagItem amTagItem) {
        AmTagDisplayValue.findAll('from AmTagDisplayValue a where a.subjectUid=? and a.amTagItem.id=?',
                [folder.uniqueId, amTagItem.id])
    }

    AmTagDisplayValue getTagValue(FmFolder folder, AmTagItem amTagItem) {
        def values = getTagValues(folder, amTagItem)
        (values && !values.empty) ? values[0] : null
    }

    List<ConceptCode> getCodes(AmTagItem amTagItem) {
        ConceptCode.findAll('from ConceptCode where codeTypeName=? order by codeName', [amTagItem.codeTypeName])
    }

    def getViewValues(String fieldValue) {
        log.info "MetaDataService.getViewValues called"

        def terms = fieldValue.split('\\|')
        def list = [] as List
        terms.each
                {
                    def bioDataId = BioData.find('from BioData where uniqueId=?', it)?.id
                    if (bioDataId) {
                        list.add(bioDataId)
                    }
                }

        log.info "list = " + list

        def tagValues = ""
        if (list.size() > 0) {
            tagValues = ConceptCode.executeQuery('from ConceptCode as cc where id in(:list)', [list: list])
        }

        log.info "tagValues = " + tagValues

        return tagValues
    }

}
