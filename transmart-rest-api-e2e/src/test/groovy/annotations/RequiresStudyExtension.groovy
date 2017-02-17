package annotations

import base.ContentTypeFor
import groovyx.net.http.HTTPBuilder
import org.spockframework.runtime.GroovyRuntimeUtil
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.extension.ExtensionException
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.ISkippable
import org.spockframework.runtime.model.SpecInfo

import static base.RestHelper.*
import static config.Config.*

class RequiresStudyExtension extends AbstractAnnotationDrivenExtension<RequiresStudy> {

    private static Set<String> loadedStudies = new HashSet<String>()

    @Override
    void visitSpecAnnotation(RequiresStudy annotation, SpecInfo spec) {
        doVisit(annotation, spec)
    }

    @Override
    void visitFeatureAnnotation(RequiresStudy annotation, FeatureInfo feature) {
        doVisit(annotation, feature)
    }

    private void doVisit(RequiresStudy annotation, ISkippable skippable) {
        Object result = studiesLoaded(annotation.value())

        if (!GroovyRuntimeUtil.isTruthy(result)) {
            skippable.setSkipped(true)
        }
    }

    private studiesLoaded(String... studies){
        if (loadedStudies?.empty) {
            try {
                println("retrieving loaded studies")
                def http = new HTTPBuilder(BASE_URL)
                def token = oauth2Authenticate(http, [username: ADMIN_USERNAME, password: ADMIN_PASSWORD])[1]

                def v1studies = get(http, [path: V1_PATH_STUDIES, acceptType: ContentTypeFor.JSON, accessToken: token]).studies as List
                loadedStudies.addAll(v1studies*.id as Set)

                def v2studies = get(http, [path: PATH_STUDIES, acceptType: ContentTypeFor.JSON, accessToken: token]).studies as List
                loadedStudies.addAll(v2studies*.studyId as Set)
                println("studies retrieved. loadedStudies: ${loadedStudies}")
            } catch (Exception e) {
                throw new ExtensionException("Failed to retrieve loadedStudies for @RequiresStudy", e);
            }
        }
        println("@RequiresStudy check")
        return loadedStudies.containsAll(studies)
    }
}
