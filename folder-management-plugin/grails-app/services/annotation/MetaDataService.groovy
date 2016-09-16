package annotation

import org.transmart.biomart.BioData
import org.transmart.biomart.ConceptCode


class MetaDataService {

    boolean transactional = true

    def serviceMethod() {

    }

    def getViewValues(fieldValue) {
        log.info "MetaDataService.getViewValues called"

        def terms = fieldValue.split('\\|')
        def list = []
        terms.each
                {
                    def bioDataId = BioData.find('from BioData where uniqueId=?', it)?.id
                    if (bioDataId) {
                        list.add(bioDataId)
                    }
                }

        log.info "list = " + list

        def tagValues = ""
        if (list.size > 0) {
            tagValues = ConceptCode.executeQuery('from ConceptCode as cc where id in(:list)', [list: list])
        }

        log.info "tagValues = " + tagValues

        return tagValues
    }


}
