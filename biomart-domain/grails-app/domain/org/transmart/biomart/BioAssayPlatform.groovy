
package org.transmart.biomart

class BioAssayPlatform {
    Long id
    String name
    String platformVersion
    String description
    String organism
    String accession
    String array
    String vendor
    String platformType
    String platformTechnology

    String uniqueId

    static transients = ['uniqueId', 'fullName']

    static mapping = {
        table 'BIO_ASSAY_PLATFORM'
        version false
        cache true
        id generator: 'sequence', params: [sequence: 'SEQ_BIO_DATA_ID']
        columns {
            id column: 'BIO_ASSAY_PLATFORM_ID'
            name column: 'PLATFORM_NAME'
            platformVersion column: 'PLATFORM_VERSION'
            description column: 'PLATFORM_DESCRIPTION'
            organism column: 'PLATFORM_ORGANISM'
            accession column: 'PLATFORM_ACCESSION'
            array column: 'PLATFORM_ARRAY'
            vendor column: 'PLATFORM_VENDOR'
            platformType column: 'PLATFORM_TYPE'
            platformTechnology column: 'PLATFORM_TECHNOLOGY'
        }
    }

    static constraints = {
        name(nullable: true, maxSize: 400)
        platformVersion(nullable: true, maxSize: 400)
        description(nullable: true, maxSize: 2000)
        platformType(nullable: true)
        platformTechnology(nullable: true)
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


    String getFullName() {
        return (platformType + "/" + platformTechnology + "/" + vendor + "/" + name)

    }

/**
 * Find concept code by its uniqueId
 * @param uniqueId
 * @return BioAssayPlatform with matching uniqueId or null, if match not found.
 */

    static BioAssayPlatform findByUniqueId(String uniqueId) {
        BioAssayPlatform cc;
        BioData bd = BioData.findByUniqueId(uniqueId);
        if (bd != null) {
            cc = BioAssayPlatform.get(bd.id);
        }
        return cc;
    }
}
