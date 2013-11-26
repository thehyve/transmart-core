import lib.AnnotationRTPCR
import lib.clinical.ClinicalFiles
import lib.soft.SoftParser
import lib.soft.SoftPlatformEntity

SoftParser parser = new SoftParser(new File(args[0]))
String clinicalStudyId = args[1]

SoftPlatformEntity platform = parser.getEntityOfType(SoftPlatformEntity)

AnnotationRTPCR annotationRTPCR
try {
    annotationRTPCR = new AnnotationRTPCR()

    annotationRTPCR.platformId = platform['Platform_geo_accession']
    annotationRTPCR.title = platform['Platform_title']
    annotationRTPCR.organism = platform['Platform_organism']
    annotationRTPCR.table = platform.table

    annotationRTPCR.writeFiles()
} finally {
    annotationRTPCR.close()
}

ClinicalFiles clinicalFiles = new ClinicalFiles()
clinicalFiles.studyId = clinicalStudyId
clinicalFiles.parser = parser

clinicalFiles.writeFiles()
