package org.transmartproject.db.multidimquery

import com.google.common.collect.ImmutableMap
import grails.orm.HibernateCriteriaBuilder
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.InheritConstructors
import groovy.transform.TupleConstructor
import org.apache.commons.lang.NotImplementedException
import org.hibernate.SessionFactory
import org.transmartproject.core.IterableResult
import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.exceptions.DataInconsistencyException
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.multidimquery.Property
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.clinical.Query
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.TrialVisit

import org.transmartproject.db.i2b2data.ConceptDimension as I2b2ConceptDimensions
import org.transmartproject.db.i2b2data.VisitDimension as I2b2VisitDimension
import org.transmartproject.db.i2b2data.PatientDimension as I2b2PatientDimension
import org.transmartproject.db.i2b2data.Study as I2B2Study
import org.transmartproject.db.support.ChoppedInQueryCondition

import static org.transmartproject.core.multidimquery.Dimension.*
import static org.transmartproject.core.multidimquery.Dimension.Size.*
import static org.transmartproject.core.multidimquery.Dimension.Density.*
import static org.transmartproject.core.multidimquery.Dimension.Packable.*

/* Not sure if the generic parameters are worth it. They cannot be used fully due to implementing a non-generic
interface, and we need to know the reified element type to check that the right types are used. And they need to be
typed twice for every dimension due to the use of generic traits.
 */
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

    static {
        builtinDimensions.values().each { it.verify() }
    }

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

    protected <T> T getKey(Map map, String alias) {
        def res = map.getOrDefault(alias, this)
        if(res.is(this)) throw new IllegalArgumentException("Resultmap $map does not contain key $alias")
        (T) res
    }
    abstract def selectIDs(Query query)

    abstract ELKey getElementKey(Map result)


    /** The externally visible element type, only for serializable dimensions */
    abstract Class<? extends Serializable> getElementType()
    /** The internal element type */
    protected abstract Class getElemType()
    /**
     * @return A List<String> or a Map<String,String> of properties on the dimension elements to serialize. If a map,
     * the keys specify the field names in the serialization, the values the property names on the elements. If a
     * string, field names and property names are the same.
     */
    protected abstract List getElemFields()

    // These are implemented in SerializableElemDim or CompoundElemDim
    abstract boolean getElementsSerializable()
    abstract def asSerializable(element)
    abstract List resolveElements(List elementKeys)
    abstract def resolveElement(key)

    // This should live in CompositeElemDim, but @Lazy doesn't work in traits
    @Lazy ImmutableMap<String,PropertyImpl> elementFields = ImmutableMap.copyOf(
        elemFields.collectEntries {
            Property prop
            if(it instanceof String) {
                MetaProperty mp = elemType.metaClass.getMetaProperty(it)
                if(mp == null) throw new RuntimeException("property $it does not exist on type $elemType")
                prop = makeProperty(it, it, mp.type)
            } else if(it instanceof Property) {
                prop = (Property) it
            } else throw new RuntimeException("not String or Property: $it")

            [prop.name, prop]
        }
    )

    @Override String toString() {
        this.class.simpleName
    }

    /** Dimensions may override this to use custom Property implementations. */
    protected abstract PropertyImpl makeProperty(String field, String propertyname, Class type)

    static boolean isSerializableType(Class t) {
        [Number, String, Date].any { it.isAssignableFrom(t) }
    }
    /**
     * Differences between serializable and non-serializable element types are implemented by extending the
     * SerializableElemDim trait. This method verifies that the element type is consistent with the usage of this trait.
     */
    final void verify() {
        assert elemType != null
        boolean serializable = isSerializableType(elemType)
        assert elementsSerializable == serializable
        assert (elemFields == null) == serializable
        assert (elementFields == null) == serializable
    }
}

@CompileStatic @TupleConstructor
class PropertyImpl implements Property {
    final String name; final String propertyName; final Class type
    def get(element) { element.getAt(propertyName) }
}


/**
 * Implement this in dimensions that have a serializable element type (Number, String, or Date)
 * @param <ELTKey> The type of both the key and the element, which must be the same
 */
@CompileStatic
trait SerializableElemDim<ELTKey> {
    boolean getElementsSerializable() { true }

    abstract Class getElemType()

    Class<? extends Serializable> getElementType() { getElemType() }
    List getElemFields() { null }
    ImmutableMap<String,Class> getElementFields() { null }
    PropertyImpl makeProperty(String field, String propertyname, Class type) {
        throw new UnsupportedOperationException("makeProperty not available for serializable element dimensions: $this")
    }

    def asSerializable(/*ELTKey*/ element) { element }

    List<ELTKey> resolveElements(List/*<ELTKey>*/ elementKeys) { elementKeys }
    ELTKey resolveElement(/*ELTKey*/ key) { key }
}

/**
 * Implement this in dimensions that have a compound element type. The implement the abstract methods to have support
 * for the required operations.
 * @param <ELT> The type of the dimension's elements
 * @param <ELKey> The type of the dimensions elements key
 */
@CompileStatic
trait CompositeElemDim<ELT,ELKey> {
    boolean getElementsSerializable() { false }

    abstract Class getElemType()
    abstract Map<String,Property> getElementFields()

    Class<? extends Serializable> getElementType() { null }

    PropertyImpl makeProperty(String field, String propertyname, Class type) {
        new PropertyImpl(field, propertyname, type)
    }

    Map<String,Object> asSerializable(/*ELT*/ element) {
        if(!elemType.isInstance(element)) {
            throw new InvalidArgumentsException("element with wrong type passed to ${this}.asSerializable; " +
                    "expected $elemType, got type ${element.class}, value $element")
        }

        Map result = [:]
        def pogo = (GroovyObject) element
        for(prop in elementFields.values()) {
            result[prop.name] = prop.get(pogo)
        }
        result
    }

    abstract List<ELT> doResolveElements(List<ELKey> elementKeys)

    List<ELT> resolveElements(List/*<ELKey>*/ elementKeys) {
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

    /* This default implementation should be overridden for efficiency for sparse dimensions. */
    ELT resolveElement(/*ELKey*/ elementId) {
        resolveElements([elementId])[0]
    }
}


@CompileStatic @InheritConstructors
abstract class I2b2Dimension<ELT,ELKey> extends DimensionImpl<ELT,ELKey> {
    SessionFactory sessionFactory
    abstract String getAlias()
    abstract String getColumnName()

    @Override
    def selectIDs(Query query) {
        query.criteria.property columnName, alias
    }

    @Override
    ELKey getElementKey(Map result) {
        assert result.getOrDefault('modifierCd', '@') == '@'
        getKey(result, alias)
    }
}

// Nullable primary key
@CompileStatic @InheritConstructors
abstract class I2b2NullablePKDimension<ELT,ELKey> extends I2b2Dimension<ELT,ELKey> {
    abstract ELKey getNullValue()

    @Override ELKey getElementKey(Map result) {
        assert result.getOrDefault('modifierCd', '@') == '@'
        ELKey res = getKey(result, alias)
        res == nullValue ? null : res
    }
}

@CompileStatic @InheritConstructors
abstract class HighDimDimension<ELT,ELKey> extends DimensionImpl<ELT,ELKey> {
    @Override def selectIDs(Query query) {
        throw new NotImplementedException()
    }

    List<ELT> doResolveElements(List<ELKey> elementKeys) {
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
class ModifierDimension extends DimensionImpl<Object,Object> implements SerializableElemDim<Object> {
    private static Map<String,ModifierDimension> byName = [:]
    private static Map<String,ModifierDimension> byCode = [:]
    synchronized static ModifierDimension get(String name, String modifierCode, Class elementType,
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

        if(!isSerializableType(elementType)) throw new NotImplementedException(
                "Support for non-serializable modifier dimensions is not implemented: $name")
        ModifierDimension dim = new ModifierDimension(name, modifierCode, elementType, size, density, packable)
        dim.verify()
        byName[name] = dim
        byCode[modifierCode] = dim

        dim
    }

    private ModifierDimension(String name, String modifierCode, Class elementType, Size size, Density density, Packable packable) {
        super(size, density, packable)
        this.name = name
        this.modifierCode = modifierCode
        this.elemType = elementType
    }

    static final String modifierCodeField = 'modifierCd'

    final Class elemType
    final String name
    final String modifierCode

    @CompileDynamic
    @Override def selectIDs(Query query) {
        if(query.params.modifierCodes == ['@']) {
            // make sure this is added only once to the query
            query.criteria.property modifierCodeField, modifierCodeField
        }
        query.params.modifierCodes += modifierCode
    }

    @Override def getElementKey(Map result) {
        // This may not exist, if there was no modifier row for this modifier. In that case we return null
        result[name]
    }

    @Override String toString() {
        "${this.class.simpleName}(name: '$name', code: '$modifierCode', $size, $density, $packable)"
    }

    /**
     * Add the value from the modifierRow into the result
     */
    void addModifierValue(Map result, ProjectionMap modifierRow) {
        assert modifierRow[modifierCodeField] == modifierCode
        def modifierValue = ObservationFact.observationFactValue(
                (String) modifierRow.valueType, (String) modifierRow.textValue, (BigDecimal) modifierRow.numberValue)
        if(result.putIfAbsent(name, modifierValue) != null) {
            assert result && false, "$name already used as an alias or as a different modifier"
        }
    }
}

@CompileStatic @InheritConstructors
class PatientDimension extends I2b2Dimension<Patient, Long> implements CompositeElemDim<Patient, Long> {
    Class elemType = Patient
    List elemFields = ["id", "trial", "inTrialId", "birthDate", "deathDate",
                      "age", "race", "maritalStatus", "religion",
                      new PropertyImpl('sex', 'sex', String) {
                          @Override def get(element) { super.get(element).toString() }
                      }]

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
        HibernateCriteriaBuilder builder = org.transmartproject.db.i2b2data.PatientDimension.createCriteria()
        def choppedInQueryCondition = new ChoppedInQueryCondition('patient_num', elementKeys)
        choppedInQueryCondition.addConstraintsToCriteriaByColumnName(builder)
        def res = choppedInQueryCondition.getResultList(builder)

        Map<Long,I2b2PatientDimension> ids = new HashMap(res.size(), 1.0f)
        for (object in res) {
            ids[object.id] = object
        }
        res.clear()
        for (key in elementKeys) {
            res << ids[key]
        }
        res
    }
}

@CompileStatic @InheritConstructors
class ConceptDimension extends I2b2NullablePKDimension<I2b2ConceptDimensions, String> implements
        CompositeElemDim<I2b2ConceptDimensions, String> {
    Class elemType = I2b2ConceptDimensions
    List elemFields = ["conceptPath", "conceptCode"]
    String name = 'concept'
    String alias = 'conceptCode'
    String columnName = 'conceptCode'
    String nullValue = '@'

    @CompileDynamic
    @Override List<I2b2ConceptDimensions> doResolveElements(List<String> elementKeys) {
        HibernateCriteriaBuilder builder = I2b2ConceptDimensions.createCriteria()
        def choppedInQueryCondition = new ChoppedInQueryCondition('concept_cd', elementKeys)
        choppedInQueryCondition.addConstraintsToCriteriaByColumnName(builder)
        def res = choppedInQueryCondition.getResultList(builder)
        res.sort{ elementKeys.indexOf(it.conceptCode) }
        res
    }
}

@CompileStatic @InheritConstructors
class TrialVisitDimension extends I2b2Dimension<TrialVisit, Long> implements CompositeElemDim<TrialVisit, Long> {
    Class elemType = TrialVisit
    List elemFields = ["id", "relTimeLabel", "relTimeUnit", "relTime"]
    String name = 'trial visit'
    String alias = 'trialVisitId'
    String columnName = 'trialVisit.id'

    @CompileDynamic
    @Override List<TrialVisit> doResolveElements(List<Long> elementKeys) {
        HibernateCriteriaBuilder builder = TrialVisit.createCriteria()
        def choppedInQueryCondition = new ChoppedInQueryCondition('trial_visit_num', elementKeys)
        choppedInQueryCondition.addConstraintsToCriteriaByColumnName(builder)
        def res = choppedInQueryCondition.getResultList(builder)
        res.sort{ elementKeys.indexOf(it.id) }
        res
    }
}

@CompileStatic @InheritConstructors
class StudyDimension extends I2b2Dimension<MDStudy, Long> implements CompositeElemDim<MDStudy, Long> {
    Class elemType = MDStudy
    List elemFields = ["name"]
    String name = 'study'
    String alias = 'studyName'
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
        HibernateCriteriaBuilder builder = I2B2Study.createCriteria()
        def choppedInQueryCondition = new ChoppedInQueryCondition('study_num', elementKeys)
        choppedInQueryCondition.addConstraintsToCriteriaByColumnName(builder)
        def res = choppedInQueryCondition.getResultList(builder)
        res.sort{ elementKeys.indexOf(it.id) }
        res
    }
}


@CompileStatic @InheritConstructors
class StartTimeDimension extends I2b2NullablePKDimension<Date,Date> implements SerializableElemDim<Date> {
    Class elemType = Date
    String name = 'start time'

    final static Date EMPTY_DATE = Date.parse('yyyy-MM-dd HH:mm:ss', '0001-01-01 00:00:00')

    String alias = 'startDate'
    String columnName = 'startDate'
    Date nullValue = EMPTY_DATE
}

@CompileStatic @InheritConstructors
class EndTimeDimension extends I2b2Dimension<Date,Date> implements SerializableElemDim<Date> {
    Class elemType = Date
    String name = 'end time'
    String alias = 'endDate'
    String columnName = 'endDate'
}

@CompileStatic @InheritConstructors
class LocationDimension extends I2b2Dimension<String,String> implements SerializableElemDim<String> {
    Class elemType = String
    String name = 'location'
    String alias = 'location'
    String columnName = 'locationCd'
}

@CompileStatic @InheritConstructors
class VisitDimension extends DimensionImpl<I2b2VisitDimension, VisitKey> implements
        CompositeElemDim<I2b2VisitDimension, VisitKey> {
    Class elemType = I2b2VisitDimension
    List elemFields = ["patientInTrialId", "encounterNum", "activeStatusCd", "startDate", "endDate", "inoutCd",
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

    @Override VisitKey getElementKey(Map result) {
        BigDecimal encounterNum = (BigDecimal) getKey(result, alias)
        encounterNum == minusOne ? null : new VisitKey(encounterNum, (Long) result.patientId)
    }

    @CompileDynamic
    @Override List<I2b2VisitDimension> doResolveElements(List<VisitKey> elementKeys) {
        (List) I2b2VisitDimension.withCriteria {
            or {
                elementKeys.each { VisitKey key ->
                    and {
                        eq 'encounterNum', key.encounterNum
                        eq 'patient.id', key.patientId
                    }
                }
            }
        }
    }

    // The same as @Immutable, but @Immutable generates some rather dynamic/inefficient constructors and a toString()
    // I don't quite like
    @EqualsAndHashCode
    static private class VisitKey {
        final BigDecimal encounterNum
        final Long patientId

        VisitKey(BigDecimal encounterNum, Long patientId) {
            this.encounterNum = encounterNum; this.patientId = patientId
        }

        String toString() { "VisitKey(encounterNum: $encounterNum, patientId: $patientId)" }
    }
}

@CompileStatic @InheritConstructors
class ProviderDimension extends I2b2NullablePKDimension<String,String> implements SerializableElemDim<String> {
    Class elemType = String
    String name = 'provider'
    String alias = 'provider'
    String columnName = 'providerId'
    String nullValue = '@'
}

@CompileStatic @InheritConstructors
class AssayDimension extends HighDimDimension<Assay,Long> implements CompositeElemDim<Assay, Long> {
    Class elemType = Assay
    List elemFields = ['id', 'sampleCode',
        new PropertyImpl('sampleTypeName', null, String) {
            def get(element) { ((Assay) element).sampleType?.label } },
        new PropertyImpl('platform', null, String) {
            def get(element) { ((Assay) element).platform?.id } },
    ]
    String name = 'assay'
}

// TODO: Expose the other Assay properties as the proper dimensions. Their structure should as much as possible be
// the same as the dimensional structure of native hypercube highdim implementations. We currently only do that with
// the patient.
// The tabular assay also includes timepointName and tissueTypeName in the rest api serialization

// TODO: Expose type-specific biomarker properties. E.g. ProbeRow has a probe, geneSymbol and geneId property

@CompileStatic @InheritConstructors
class BioMarkerDimension extends HighDimDimension<HddTabularResultHypercubeAdapter.BioMarkerAdapter,Object> implements
        CompositeElemDim<HddTabularResultHypercubeAdapter.BioMarkerAdapter,Object> {
    Class elemType = HddTabularResultHypercubeAdapter.BioMarkerAdapter
    List elemFields = ['label', 'biomarker']
    String name = 'biomarker'
}

@CompileStatic @InheritConstructors
class ProjectionDimension extends HighDimDimension<String,String> implements SerializableElemDim<String> {
    Class elemType = String
    String name = 'projection'
}
