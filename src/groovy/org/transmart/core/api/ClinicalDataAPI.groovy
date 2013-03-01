package org.transmart.core.api
import org.transmart.core.data.clinical.*

interface ClinicalDataAPI {

   Collection<Sample> getSamples(Trial trial)          // GET /rest/sample/1 - GET /rest/sample - GET /trial/samples
   //Collection<Sample> getSamples(Assay assay)

   //Collection<Sample> getTrialSamples()

   //Sample createSample(Map args)

}