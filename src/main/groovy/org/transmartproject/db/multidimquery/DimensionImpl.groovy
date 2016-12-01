package org.transmartproject.db.multidimquery

import grails.util.Pair
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.apache.commons.lang.NotImplementedException
import org.transmartproject.core.IterableResult
import org.transmartproject.core.exceptions.DataInconsistencyException
import org.transmartproject.core.multidimquery.Dimension
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.clinical.Query
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.TrialVisit

import static org.transmartproject.core.multidimquery.Dimension.*
import static org.transmartproject.core.multidimquery.Dimension.Size.*
import static org.transmartproject.core.multidimquery.Dimension.Density.*
import static org.transmartproject.core.multidimquery.Dimension.Packable.*


abstract class DimensionImpl implements Dimension {

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


    DimensionImpl(Size size, Density density, Packable packable) {
        this.size = size
        this.density = density
        this.packable = packable
    }

    @Override IterableResult<Object> getElements(Collection<Study> studies) {
        throw new NotImplementedException()
    }

    abstract def selectIDs(Query query)

    @CompileStatic
    abstract def getElementKey(Map result)

    List<Object> resolveElements(List elementKeys) {
        if (elementKeys.size() == 0) return []

        List<Object> results = doResolveElements(elementKeys)
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

    abstract List doResolveElements(List elementKeys)

    /* This default implementation should be overridden for efficiency for non-packable dimensions.
     */

    def resolveElement(elementId) {
        resolveElements([elementId])[0]
    }

    @Override String toString() {
        this.class.simpleName
    }
}

@InheritConstructors
@CompileStatic
abstract class I2b2Dimension extends DimensionImpl {
    abstract String getAlias()
    abstract String getColumnName()

    @Override
    def selectIDs(Query query) {
        query.criteria.property columnName, alias
    }

    @Override
    def getElementKey(Map result) {
        assert result.getOrDefault('modifierCd', '@') == '@'
        result[alias]
    }
}

// Nullable primary key
@CompileStatic @InheritConstructors
abstract class I2b2NullablePKDimension extends I2b2Dimension {
    abstract def getNullValue()

    @Override
    def getElementKey(Map result) {
        assert result.getOrDefault('modifierCd', '@') == '@'
        def res = result[alias]
        res == nullValue ? null : res
    }
}

@InheritConstructors
abstract class HighDimDimension extends DimensionImpl {
    @Override
    def selectIDs(Query query) {
        throw new NotImplementedException()
    }

    @Override
    List doResolveElements(List elementKeys) {
        throw new NotImplementedException()
    }

    @Override @CompileStatic
    def getElementKey(Map result) {
        throw new NotImplementedException()
    }
}


class ModifierDimension extends DimensionImpl {
    private static Map<String,ModifierDimension> byName = [:]
    private static Map<String,ModifierDimension> byCode = [:]
    synchronized static ModifierDimension get(String name, String modifierCode,
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

        ModifierDimension dim = new ModifierDimension(name, modifierCode, size, density, packable)
        byName[name] = dim
        byCode[modifierCode] = dim

        dim
    }

    private ModifierDimension(String name, String modifierCode, Size size, Density density, Packable packable) {
        super(size, density, packable)
        this.name = name
        this.modifierCode = modifierCode
    }

    static final String modifierCodeField = 'modifierCd'

    final String name
    final String modifierCode

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

    @Override List doResolveElements(List elementKeys) {
        return elementKeys
    }

    @Override def resolveElement(key) {
        key
    }

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

@InheritConstructors
class PatientDimension extends I2b2Dimension {
    String alias = 'patientId'
    String columnName = 'patient.id'

    @Override def selectIDs(Query query) {
        if(query.params.patientSelected) return
        super.selectIDs(query)
        query.params.patientSelected = true
    }

    @Override List doResolveElements(List elementKeys) {
        org.transmartproject.db.i2b2data.PatientDimension.getAll(elementKeys)
    }
}

@InheritConstructors
class ConceptDimension extends I2b2NullablePKDimension {
    String alias = 'conceptCode'
    String columnName = 'conceptCode'
    String nullValue = '@'

    @Override List doResolveElements(List elementKeys) {
        org.transmartproject.db.i2b2data.ConceptDimension.findAllByConceptCodeInList(elementKeys)
    }
}

@InheritConstructors
class TrialVisitDimension extends I2b2Dimension {
    String alias = 'trialVisitId'
    String columnName = 'trialVisit.id'

    @Override List doResolveElements(List elementKeys) {
        TrialVisit.getAll(elementKeys)
    }
}

@InheritConstructors
class StudyDimension extends I2b2Dimension {
    String alias = 'studyId'
    String getColumnName() {throw new UnsupportedOperationException()}

    def selectIDs(Query query) {
        query.criteria.with {
            trialVisit {
                property 'study.id', alias
            }
        }
    }

    @Override List doResolveElements(List elementKeys) {
        org.transmartproject.db.i2b2data.Study.getAll(elementKeys)
    }
}


@InheritConstructors
class StartTimeDimension extends I2b2NullablePKDimension {

    final static Date EMPTY_DATE = Date.parse('yyyy-MM-dd HH:mm:ss', '0001-01-01 00:00:00')

    String alias = 'startDate'
    String columnName = 'startDate'
    Date nullValue = EMPTY_DATE

    @Override List doResolveElements(List elementKeys) {
        elementKeys
    }
}

@InheritConstructors
class EndTimeDimension extends I2b2Dimension {
    String alias = 'endDate'
    String columnName = 'endDate'

    @Override List doResolveElements(List elementKeys) {
        elementKeys
    }
}

@InheritConstructors
class LocationDimension extends I2b2Dimension {
    String alias = 'location'
    String columnName = 'locationCd'

    @Override List doResolveElements(List elementKeys) {
        elementKeys
    }
}

@InheritConstructors
class VisitDimension extends DimensionImpl {
    static String alias = 'encounterNum'

    @Override
    def selectIDs(Query query) {
        query.criteria.with {
            if(!query.params.patientSelected) {
                property 'patient.id', 'patientId'
                query.params.patientSelected = true
            }
            property 'encounterNum', alias
        }
    }

    static private BigDecimal minusOne = new BigDecimal(-1)

    @Override @CompileStatic
    def getElementKey(Map result) {
        BigDecimal encounterNum = (BigDecimal) result[alias]
        encounterNum == minusOne ? null : new Pair(encounterNum, result.patientId)
    }

    @Override
    List doResolveElements(List elementKeys) {
        (List) org.transmartproject.db.i2b2data.VisitDimension.withCriteria {
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

@InheritConstructors
class ProviderDimension extends I2b2NullablePKDimension {
    String alias = 'provider'
    String columnName = 'providerId'
    String nullValue = '@'

    List doResolveElements(List elementKeys) {
        elementKeys
    }
}

@InheritConstructors
class AssayDimension extends HighDimDimension {
}

@InheritConstructors
class BioMarkerDimension extends HighDimDimension {
}

@InheritConstructors
class ProjectionDimension extends HighDimDimension {
}