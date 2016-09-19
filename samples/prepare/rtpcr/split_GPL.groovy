/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-data.
 *
 * Transmart-data is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-data.  If not, see <http://www.gnu.org/licenses/>.
 */

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
