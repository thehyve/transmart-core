package org.transmartproject.db.dataquery.highdim

import org.transmartproject.db.biomarker.BioMarkerCoreDb
import org.transmartproject.db.search.SearchKeywordCoreDb

import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createBioMarkers
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.createSearchKeywordsForBioMarkers
import static org.transmartproject.db.dataquery.highdim.HighDimTestData.save

class SampleBioMarkerTestData {

    List<BioMarkerCoreDb> geneBioMarkers = createBioMarkers(-100L, [
            [ name: 'BOGUSCPO',
                    description: 'carboxypeptidase O',
                    primaryExternalId: '-130749' ],
            [ name: 'BOGUSRQCD1',
                    description: 'RCD1 required for cell differentiation1 homolog (S. pombe)',
                    primaryExternalId: '-9125' ],
            [ name: 'BOGUSVNN3',
                    description: 'vanin 3',
                    primaryExternalId: '-55350' ],
            [ name: 'BOGUSCPOCORREL',
                    description: 'Bogus gene associated with BOGUSCPO',
                    primaryExternalId: '-130750']])

    List<SearchKeywordCoreDb> geneSearchKeywords =
        createSearchKeywordsForBioMarkers(geneBioMarkers, -500L)



    void saveGeneData() {
        save geneBioMarkers
        save geneSearchKeywords
    }
}
