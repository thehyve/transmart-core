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
    public static final Map<String, ImmutableMap<String, Class>> SERIALIZABLES = ["ConceptDimension": CONCEPT,
                                                                                  "PatientDimension": PATIENT]
    // TODO: fill in the rest of the serializable fields for all other dimensions
}
