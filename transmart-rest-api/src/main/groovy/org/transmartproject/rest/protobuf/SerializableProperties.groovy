package org.transmartproject.rest.protobuf

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet

/**
 * Created by piotrzakrzewski on 03/11/2016.
 */
class SerializableProperties {
    public static final Set<String> PATIENT = ["birthDate", "deathDate",
                                                        "age", "race", "maritalStatus",
                                                        "religion", "sourcesystemCd", "sexCd"]
    public static final Set<String> CONCEPT = ["conceptPath", "conceptCode"]
    public static final Set<String> STUDY = ["studyId", "bioExperimentId", "secureObjectToken"]
    public static final Set<String> TRIAL_VISIT = ["relTimeUnit", "relTime", "relTimeLabel", "study"]
    public static final Set<String> START_DATE = ["time"]
    public static final Map<String, Set<String>> SERIALIZABLES = [
            "ConceptDimension": CONCEPT,
            "PatientDimension": PATIENT,
            "TrialVisitDimension": TRIAL_VISIT,
            "StartDateDimension": START_DATE,
            "StudyDimension": STUDY,
    ]
    // TODO: fill in the rest of the serializable fields for all other dimensions
}
