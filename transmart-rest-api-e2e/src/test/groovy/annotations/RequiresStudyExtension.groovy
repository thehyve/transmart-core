package annotations

import base.RestHelper
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.extension.ExtensionException
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.ISkippable
import org.spockframework.runtime.model.SpecInfo

import static base.ContentTypeFor.JSON
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
        if (!studiesLoaded(annotation.value())) {
            skippable.setSkipped(true)
        }
    }

    private studiesLoaded(String... studies) {
        if (loadedStudies?.empty) {
            try {
                println("retrieving studies loaded on ${BASE_URL}")
                def v1studies = RestHelper.get(testContext, [path: V1_PATH_STUDIES, acceptType: JSON, user: ADMIN_USER]).studies as List
                loadedStudies.addAll(v1studies*.id as Set)

                def v2studies = RestHelper.get(testContext, [path: PATH_STUDIES, acceptType: JSON, user: ADMIN_USER]).studies as List
                loadedStudies.addAll(v2studies*.studyId as Set)
                println("studies retrieved. loadedStudies: ${loadedStudies}")
            } catch (Exception e) {
                throw new ExtensionException("Failed to retrieve loadedStudies for @RequiresStudy", e);
            }
        }
        return loadedStudies.containsAll(studies)
    }
}