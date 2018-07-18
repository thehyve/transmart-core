package annotations

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.ISkippable
import org.spockframework.runtime.model.SpecInfo

import static config.Config.IS_V1_API_SUPPORTED

class RequiresV1ApiSupportExtension extends AbstractAnnotationDrivenExtension<RequiresV1ApiSupport> {

    @Override
    void visitSpecAnnotation(RequiresV1ApiSupport annotation, SpecInfo spec) {
        doVisit(annotation, spec)
    }

    @Override
    void visitFeatureAnnotation(RequiresV1ApiSupport annotation, FeatureInfo feature) {
        doVisit(annotation, feature)
    }

    private static void doVisit(RequiresV1ApiSupport annotation, ISkippable skippable) {
        if ((annotation.value() && !IS_V1_API_SUPPORTED)
                || (!annotation.value() && IS_V1_API_SUPPORTED)) {
            skippable.setSkipped(true)
        }
    }

}
