/**
 * Created with IntelliJ IDEA.
 * User: kees
 * Date: 27-02-13
 * Time: 14:39
 * To change this template use File | Settings | File Templates.
 */

package transmart.core.api

interface ClinicalDataAPI {

   Collection<Sample> getSamples(Trial trial)
   Sample createSample(Map args)

}
