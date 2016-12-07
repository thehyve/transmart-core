package org.transmartproject.db.multidimquery

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import grails.util.Pair
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.apache.commons.lang.NotImplementedException
import org.transmartproject.core.IterableResult
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.exceptions.DataInconsistencyException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.clinical.Query
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.TrialVisit

import org.transmartproject.db.i2b2data.ConceptDimension as I2b2ConceptDimensions
import org.transmartproject.db.i2b2data.VisitDimension as I2b2VisitDimension

import static org.transmartproject.core.multidimquery.Dimension.*
import static org.transmartproject.core.multidimquery.Dimension.Size.*
import static org.transmartproject.core.multidimquery.Dimension.Density.*
import static org.transmartproject.core.multidimquery.Dimension.Packable.*


@CompileStatic
abstract class DimensionImpl<ELT,ELKey> implements Dimension {

    final Size size
    final Density density
    final Packable packable


    // Size is currently not used.
    //
    // DENSE indicates that the dimension will be indexed in the hypercube, SPARSE dimensions are inlined
    //
    // Packable dimensions can be (profitably) packed into arrays in protobuf serialization. Requirements for being
    // packable are:
    //   - Dimension is dense
    //   - Values are of the same type (so dimensions that determine the type can not be packable, e.g. concept)
    //   - There are typically at least a dozen elements in a result set, if not it is usually more profitable to
    //     pack a dimension with more elements.

    static final StudyDimension STUDY =            new StudyDimension(SMALL, DENSE, NOT_PACKABLE)
    static final ConceptDimension CONCEPT =        new ConceptDimension(MEDIUM, DENSE, NOT_PACKABLE)
    static final PatientDimension PATIENT =        new PatientDimension(LARGE, DENSE, PACKABLE)
    static final VisitDimension VISIT =            new VisitDimension(MEDIUM, DENSE, PACKABLE)
    static final StartTimeDimension START_TIME =   new StartTimeDimension(LARGE, SPARSE, NOT_PACKABLE)
    static final EndTimeDimension END_TIME =       new EndTimeDimension(LARGE, SPARSE, NOT_PACKABLE)
    static final LocationDimension LOCATION =      new LocationDimension(MEDIUM, SPARSE, NOT_PACKABLE)
    static final TrialVisitDimension TRIAL_VISIT = new TrialVisitDimension(SMALL, DENSE, PACKABLE)
    static final ProviderDimension PROVIDER =      new ProviderDimension(SMALL, DENSE, NOT_PACKABLE)

    // Todo: implement sample dimension as a marker for studies that can have multiple samples
    //static final DimensionImpl SAMPLE =         new SampleDimension(SMALL, DENSE, NOT_PACKABLE)

    static final BioMarkerDimension BIOMARKER =    new BioMarkerDimension(LARGE, DENSE, PACKABLE)
    static final AssayDimension ASSAY =            new AssayDimension(LARGE, DENSE, PACKABLE)
    static final ProjectionDimension PROJECTION =  new ProjectionDimension(SMALL, DENSE, NOT_PACKABLE)

    // NB: This map only contains the builtin dimensions! To get a dimension that is not necessarily builtin
    // use DimensionDescription.findByName(name).dimension
    private static final ImmutableMap<String,DimensionImpl> builtinDimensions = ImmutableMap.copyOf([
            (STUDY.name)      : STUDY,
            (CONCEPT.name)    : CONCEPT,
            (PATIENT.name)    : PATIENT,
            (VISIT.name)      : VISIT,
            (START_TIME.name) : START_TIME,
            (END_TIME.name)   : END_TIME,
            (LOCATION.name)   : LOCATION,
            (TRIAL_VISIT.name): TRIAL_VISIT,
            (PROVIDER.name)   : PROVIDER,
//            (SAMPLE.name) : SAMPLE,

            (BIOMARKER.name) : BIOMARKER,
            (ASSAY.name)     : ASSAY,
            (PROJECTION.name): PROJECTION,
    ])

    static getBuiltinDimension(String name) { builtinDimensions.get(name) }
    static boolean isBuiltinDimension(String name) { builtinDimensions.containsKey(name) }

    DimensionImpl(Size size, Density density, Packable packable) {
        this.size = size
        this.density = density
        this.packable = packable
    }

    @Override abstract String getName()

    @Override IterableResult<ELT> getElements(Collection<Study> studies) {
        throw new NotImplementedException()
    }

    abstract def selectIDs(Query query)

    abstract ELKey getElementKey(Map result)

    @Override List<ELT> resolveElements(List/*<ELKey>*/ elementKeys) {
        if (elementKeys.size() == 0) return []

        List<ELT> results = doResolveElements(elementKeys)
        int keysSize = elementKeys.size()
        int resultSize = results.size()
        if (keysSize == resultSize) return results

        // Check for duplicate keys or data error
        if (new HashSet(elementKeys).size() != keysSize) {
            throw new IllegalArgumentException("list of element keys contains duplicates, this is not allowed: " +
                    "$elementKeys")
        } else if (resultSize < keysSize) {
            throw new DataInconsistencyException("Unable to find ${this.class.simpleName} elements for all keys, this" +
                    " may be a database inconsitency.\nkeys: $elementKeys\nelements: $results")
        } else { // resultSize > keysSize
            throw new DataInconsistencyException("Duplicate ${this.class.simpleName} elements found. Elements must " +
                    "have unique keys. keys: $elementKeys")
        }
    }

    abstract List<ELT> doResolveElements(List<ELKey> elementKeys)

    /* This default implementation should be overridden for efficiency for non-packable dimensions. */
    @Override ELT resolveElement(/*ELKey*/ elementId) {
        resolveElements([elementId])[0]
    }

    // This default implementation of asSerializable works for compound element types. For simple element types that
    // themselves are already a serializable type (Number, Date, or String), the dimension should extend
    // SerializableType which provides a trivial implementation for that.
    abstract Class getElementType()
    abstract List<String> getElementProperties()
    final static List<Class> serializableTypes = ImmutableList.copyOf([Number, String, Date])

    // Too bad @Lazy doesn't work with primitive types
    @Lazy private Boolean elementTypeIsSerializable = serializableTypes.any { elementType in it }

    @Override def asSerializable(/*ELT*/ element) {
        if(!elementType.isInstance(element)) {
            throw new InvalidArgumentsException("element with wrong type passed to ${this}.asSerializable; " +
                    "expected $elementType, got type ${element.class}, value $element")
        }
        assert !elementTypeIsSerializable :
            "If the element type is serializable this dimension should inherit SerializableType which overrides this method"
        // if(elementTypeIsSerializable) return element

        Map result = [:]
        List<String> properties = elementProperties
        def pogo = (GroovyObject) element
        for(prop in properties) {
            result[prop] = pogo.getProperty(prop)
        }
        result
    }

    @Override String toString() {
        this.class.simpleName
    }
}

/**
 * Implement this in dimensions that have a serializable element type (Number, String, or Date), rather than a
 * compound element type
 */
@CompileStatic
trait SerializableType {
    List<String> getElementProperties() { throw new UnsupportedOperationException("${this}.getElementProperties()") }
    def asSerializable(/*ELT*/ element) { element }
}


@CompileStatic @InheritConstructors
abstract class I2b2Dimension<ELT,ELKey> extends DimensionImpl<ELT,ELKey> {
    abstract String getAlias()
    abstract String getColumnName()

    @Override
    def selectIDs(Query query) {
        query.criteria.property columnName, alias
    }

    @Override
    ELKey getElementKey(Map result) {
        assert result.getOrDefault('modifierCd', '@') == '@'
        result[alias]
    }
}

// Nullable primary key
@CompileStatic @InheritConstructors
abstract class I2b2NullablePKDimension<ELT,ELKey> extends I2b2Dimension<ELT,ELKey> {
    abstract ELKey getNullValue()

    @Override ELKey getElementKey(Map result) {
        assert result.getOrDefault('modifierCd', '@') == '@'
        ELKey res = result[alias]
        res == nullValue ? null : res
    }
}

@CompileStatic @InheritConstructors
abstract class HighDimDimension<ELT,ELKey> extends DimensionImpl<ELT,ELKey> {
    @Override def selectIDs(Query query) {
        throw new NotImplementedException()
    }

    @Override List<ELT> doResolveElements(List<ELKey> elementKeys) {
        throw new NotImplementedException()
    }

    @Override ELKey getElementKey(Map result) {
        throw new NotImplementedException()
    }
}


// ModifierDimension is currently only implemented for serializable types. If desired, the implementation could be
// extended to also support modifiers that link to other tables, thus leading to modifier dimensions with compound
// element types
@CompileStatic
class ModifierDimension extends DimensionImpl<Object,Object> implements SerializableType {
    Class elementType = Object
    private static Map<String,ModifierDimension> byName = [:]
    private static Map<String,ModifierDimension> byCode = [:]
    synchronized static ModifierDimension get(String name, String modifierCode, String valueType,
                                              Size size, Density density, Packable packable) {
        if(name in byName) {
            ModifierDimension dim = byName[name]
            assert dim.is(byCode[dim.modifierCode])
            if(modifierCode == dim.modifierCode && size == dim.size && density == dim.density
                    && packable == dim.packable) {
                return dim
            }

            def props = [modifierCode: modifierCode, size: size, density: density, packable: packable]
            throw new RuntimeException("attempting to create a modifier dimension with properties $props while an" +
                    " identical modifier dimension with different properties already exists: $dim")
        }
        assert !byCode.containsKey(modifierCode)

        ModifierDimension dim = new ModifierDimension(name, modifierCode, valueType, size, density, packable)
        byName[name] = dim
        byCode[modifierCode] = dim

        dim
    }

    private ModifierDimension(String name, String modifierCode, String valueType, Size size, Density density, Packable packable) {
        super(size, density, packable)
        this.name = name
        this.modifierCode = modifierCode
        if (!(valueType in [ObservationFact.TYPE_NUMBER, ObservationFact.TYPE_TEXT])) {
            throw new RuntimeException("Unsupported value type: ${valueType}. " +
                    "Should be one of [${ObservationFact.TYPE_NUMBER}, ${ObservationFact.TYPE_TEXT}].")
        }
        this.valueType = valueType
    }

    static final String modifierCodeField = 'modifierCd'

    final String name
    final String modifierCode
    final String valueType

    @CompileDynamic
    @Override def selectIDs(Query query) {
        if(query.params.modifierCodes == ['@']) {
            // make sure this is added only once to the query
            query.criteria.property modifierCodeField, modifierCodeField
        }
        query.params.modifierCodes += modifierCode
    }

    @Override def getElementKey(Map result) {
        result[name]
    }

    @Override List doResolveElements(List elementKeys) { elementKeys }

    @Override def resolveElement(key) { key }

    @Override String toString() {
        "${this.class.simpleName}(name: '$name', code: '$modifierCode', $size, $density, $packable)"
    }

    /**
     * Add the value from the modifierRow into the result
     */
    void addModifierValue(Map result, ProjectionMap modifierRow) {
        assert modifierRow[modifierCodeField] == modifierCode
        assert !result.containsKey(name), "$name already used as an alias or as a different modifier"
        result[name] = ObservationFact.observationFactValue(
                (String) modifierRow.valueType, (String) modifierRow.textValue, (BigDecimal) modifierRow.numberValue)
    }
}

@CompileStatic @InheritConstructors
class PatientDimension extends I2b2Dimension<Patient, Long> {
    Class elementType = Patient
    List<String> elementProperties = ["inTrialId", "birthDate", "deathDate",
                                      "age", "race", "maritalStatus",
                                      "religion", "sourcesystemCd", "sexCd"]
    String name = 'patient'
    String alias = 'patientId'
    String columnName = 'patient.id'

    @Override def selectIDs(Query query) {
        if(query.params.patientSelected) return
        super.selectIDs(query)
        query.params.patientSelected = true
    }

    @CompileDynamic
    @Override List<Patient> doResolveElements(List<Long> elementKeys) {
        org.transmartproject.db.i2b2data.PatientDimension.getAll(elementKeys)
    }
}

@CompileStatic @InheritConstructors
class ConceptDimension extends I2b2NullablePKDimension<I2b2ConceptDimensions, String> {
    Class elementType = I2b2ConceptDimensions
    List<String> elementProperties = ["conceptPath", "conceptCode"]
    String name = 'concept'
    String alias = 'conceptCode'
    String columnName = 'conceptCode'
    String nullValue = '@'

    @CompileDynamic
    @Override List<I2b2ConceptDimensions> doResolveElements(List<String> elementKeys) {
        List<I2b2ConceptDimensions> elements = I2b2ConceptDimensions.findAllByConceptCodeInList(elementKeys)
        elements.sort { elementKeys.indexOf(it.conceptCode) }
        elements
    }
}

@CompileStatic @InheritConstructors
class TrialVisitDimension extends I2b2Dimension<TrialVisit, Long> {
    Class elementType = TrialVisit
    List<String> elementProperties = ["relTimeLabel", "relTimeUnit", "relTime"]
    String name = 'trial visit'
    String alias = 'trialVisitId'
    String columnName = 'trialVisit.id'

    @CompileDynamic
    @Override List<TrialVisit> doResolveElements(List<Long> elementKeys) {
        TrialVisit.getAll(elementKeys)
    }
}

@CompileStatic @InheritConstructors
class StudyDimension extends I2b2Dimension<MDStudy, Long> {
    Class elementType = MDStudy
    List<String> elementProperties = ["studyId"]
    String name = 'study'
    String alias = 'studyId'
    String getColumnName() {throw new UnsupportedOperationException()}

    @CompileDynamic
    def selectIDs(Query query) {
        query.criteria.with {
            trialVisit {
                property 'study.id', alias
            }
        }
    }

    @CompileDynamic
    @Override List<MDStudy> doResolveElements(List<Long> elementKeys) {
        org.transmartproject.db.i2b2data.Study.getAll(elementKeys)
    }
}


@CompileStatic @InheritConstructors
class StartTimeDimension extends I2b2NullablePKDimension<Date,Date> implements SerializableType {
    Class elementType = Date
    String name = 'start time'

    final static Date EMPTY_DATE = Date.parse('yyyy-MM-dd HH:mm:ss', '0001-01-01 00:00:00')

    String alias = 'startDate'
    String columnName = 'startDate'
    Date nullValue = EMPTY_DATE

    @Override List doResolveElements(List elementKeys) {
        elementKeys
    }
}

@CompileStatic @InheritConstructors
class EndTimeDimension extends I2b2Dimension<Date,Date> implements SerializableType {
    Class elementType = Date
    String name = 'end time'
    String alias = 'endDate'
    String columnName = 'endDate'

    @Override List doResolveElements(List elementKeys) {
        elementKeys
    }
}

@CompileStatic @InheritConstructors
class LocationDimension extends I2b2Dimension<String,String> implements SerializableType {
    Class elementType = String
    String name = 'location'
    String alias = 'location'
    String columnName = 'locationCd'

    @Override List doResolveElements(List elementKeys) {
        elementKeys
    }
}

@CompileStatic @InheritConstructors
class VisitDimension extends DimensionImpl<I2b2VisitDimension, Pair<BigDecimal,Long>> {
    Class elementType = I2b2VisitDimension
    List<String> elementProperties = ["patientInTrialId", "activeStatus", "startDate", "endDate", "inoutCd",
                                      "locationCd"]
    String name = 'visit'
    static String alias = 'encounterNum'

    @Override def selectIDs(Query query) {
        query.criteria.with {
            if(!query.params.patientSelected) {
                property 'patient.id', 'patientId'
                query.params.patientSelected = true
            }
            property 'encounterNum', alias
        }
    }

    static private BigDecimal minusOne = new BigDecimal(-1)

    @Override Pair<BigDecimal,Long> getElementKey(Map result) {
        BigDecimal encounterNum = (BigDecimal) result[alias]
        encounterNum == minusOne ? null : new Pair(encounterNum, result.patientId)
    }

    @CompileDynamic
    @Override List<I2b2VisitDimension> doResolveElements(List<Pair<BigDecimal,Long>> elementKeys) {
        (List) I2b2VisitDimension.withCriteria {
            or {
                elementKeys.each { Pair key ->
                    and {
                        eq 'encounterNum', key.aValue
                        eq 'patient.id', key.bValue
                    }
                }
            }
        }
    }
}

@CompileStatic @InheritConstructors
class ProviderDimension extends I2b2NullablePKDimension<String,String> implements SerializableType {
    Class elementType = String
    String name = 'provider'
    String alias = 'provider'
    String columnName = 'providerId'
    String nullValue = '@'

    List doResolveElements(List elementKeys) {
        elementKeys
    }
}

@CompileStatic @InheritConstructors
class AssayDimension extends HighDimDimension<Long,Long> implements SerializableType {
    Class elementType = Long
    String name = 'assay'
}

@CompileStatic @InheritConstructors
class BioMarkerDimension extends HighDimDimension<HddTabularResultHypercubeAdapter.BioMarkerAdapter,Object> {
    Class elementType = HddTabularResultHypercubeAdapter.BioMarkerAdapter
    List<String> elementProperties = ['label', 'bioMarker']
    String name = 'biomarker'
}

@CompileStatic @InheritConstructors
class ProjectionDimension extends HighDimDimension<String,String> implements SerializableType {
    Class elementType = String
    String name = 'projection'
}
