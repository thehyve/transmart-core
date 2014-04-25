
package org.transmart.biomart

class ConceptCode {
    Long id
    String bioConceptCode
    String codeName
    String codeDescription
    String codeTypeName
    String uniqueId
    static transients = ['uniqueId']

    static hasMany = [bioDataUid: BioData]
    static mapping = {
        table 'BIO_CONCEPT_CODE'
        cache true
        version false
        id generator: 'sequence', params: [sequence: 'SEQ_BIO_DATA_ID']
        columns {
            id column: 'BIO_CONCEPT_CODE_ID'
            bioConceptCode column: 'BIO_CONCEPT_CODE'
            codeName column: 'CODE_NAME'
            codeDescription column: 'CODE_DESCRIPTION'
            codeTypeName column: 'CODE_TYPE_NAME'
        }
        bioDataUid joinTable: [name: 'BIO_DATA_UID', key: 'BIO_DATA_ID']
    }
    static constraints = {
        bioConceptCode(nullable: true, maxSize: 400)
        codeDescription(nullable: true, maxSize: 2000)
        codeTypeName(nullable: true, maxSize: 400)
    }

/**
* Use transient property to support unique ID for tagValue.
* @return tagValue's uniqueId
*/
    String getUniqueId() {
        if (uniqueId == null) {
            if (id) {
                BioData data = BioData.get(id);
                if (data != null) {
                    uniqueId = data.uniqueId
                    return data.uniqueId;
                }
                return null;
            } else {
                return null;
            }
        } else {
            return uniqueId;
        }
    }

    /**
* Find concept code by its uniqueId
* @param uniqueId
* @return concept code with matching uniqueId or null, if match not found.
*/
    static ConceptCode findByUniqueId(String uniqueId) {
        ConceptCode cc;
        BioData bd = BioData.findByUniqueId(uniqueId);
        if (bd != null) {
            cc = ConceptCode.get(bd.id);
        }
        return cc;
    }
}
