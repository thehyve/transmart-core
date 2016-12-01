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
import org.transmartproject.db.i2b2data.TrialVisit

import static org.transmartproject.core.multidimquery.Dimension.*

abstract class DimensionImpl implements Dimension {
    // TODO(jan): Which properties on dimensions do we actually need for the hypercube to work?

    final Size size
    final Density density
    final Packable packable

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
    abstract def getElementKey(ProjectionMap result)

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

    abstract List<Object> doResolveElements(List elementKeys)

    /* This default implementation should be overridden for efficiency for non-packable dimensions.
     */

    Object resolveElement(elementId) {
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
    def getElementKey(ProjectionMap result) {
        result[alias]
    }
}

// Nullable primary key
@CompileStatic @InheritConstructors
abstract class I2b2NullablePKDimension extends I2b2Dimension {
    abstract def getNullValue()

    @Override
    def getElementKey(ProjectionMap result) {
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
    List<Object> doResolveElements(List elementKeys) {
        throw new NotImplementedException()
    }

    @Override @CompileStatic
    def getElementKey(ProjectionMap result) {
        throw new NotImplementedException()
    }
}


//TODO: supporting modifier dimensions requires support for sorting, since we need to sort on the full PK except
// modifierCd in order to ensure that modifier ObservationFacts come next to their observation value
class ModifierDimension extends DimensionImpl {
    ModifierDimension(String name, String modifierCode, Size size, Density density, Packable packable) {
        super(size, density, packable)
        this.name = name
        this.modifierCode = modifierCode
    }

    String name
    String modifierCode

    @Override def selectIDs(Query query) {
        query.params.modifierCodes += modifierCode
    }

    @Override def getElementKey(ProjectionMap result) {
        throw new UnsupportedOperationException("ModifierDimension.getElementKey")
    }

    @Override List<Object> doResolveElements(List elementKeys) {
        return elementKeys
    }

    @Override String toString() {
        "${this.class.simpleName}('$name')"
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

    @Override List<Object> doResolveElements(List elementKeys) {
        org.transmartproject.db.i2b2data.PatientDimension.getAll(elementKeys)
    }
}

@InheritConstructors
class ConceptDimension extends I2b2NullablePKDimension {
    String alias = 'conceptCode'
    String columnName = 'conceptCode'
    String nullValue = '@'

    @Override List<Object> doResolveElements(List elementKeys) {
        List elements = org.transmartproject.db.i2b2data.ConceptDimension.findAllByConceptCodeInList(elementKeys)
        elements.sort { elementKeys.indexOf(it.conceptCode) }
        elements
    }
}

@InheritConstructors
class TrialVisitDimension extends I2b2Dimension {
    String alias = 'trialVisitId'
    String columnName = 'trialVisit.id'

    @Override List<Object> doResolveElements(List elementKeys) {
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

    @Override List<Object> doResolveElements(List elementKeys) {
        org.transmartproject.db.i2b2data.Study.getAll(elementKeys)
    }
}


@InheritConstructors
class StartTimeDimension extends I2b2NullablePKDimension {

    final static Date EMPTY_DATE = Date.parse('yyyy-MM-dd HH:mm:ss', '0001-01-01 00:00:00')

    String alias = 'startDate'
    String columnName = 'startDate'
    Date nullValue = EMPTY_DATE

    @Override List<Object> doResolveElements(List elementKeys) {
        elementKeys
    }
}

@InheritConstructors
class EndTimeDimension extends I2b2Dimension {
    String alias = 'endDate'
    String columnName = 'endDate'

    @Override List<Object> doResolveElements(List elementKeys) {
        elementKeys
    }
}

@InheritConstructors
class LocationDimension extends I2b2Dimension {
    String alias = 'location'
    String columnName = 'locationCd'

    @Override List<Object> doResolveElements(List elementKeys) {
        elementKeys
    }
}

@InheritConstructors
class VisitDimension extends DimensionImpl {

    @Override
    def selectIDs(Query query) {
        query.criteria.with {
            if(!query.params.patientSelected) {
                property 'patient.id', 'patientId'
                query.params.patientSelected = true
            }
            property 'encounterNum', 'encounterNum'
        }
    }

    static private BigDecimal minusOne = new BigDecimal(-1)

    @Override @CompileStatic
    def getElementKey(ProjectionMap result) {
        BigDecimal encounterNum = (BigDecimal) result.encounterNum
        encounterNum == minusOne ? null : new Pair(encounterNum, result.patientId)
    }

    @Override
    List<Object> doResolveElements(List elementKeys) {
        (List<Object>) org.transmartproject.db.i2b2data.VisitDimension.withCriteria {
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

    List<Object> doResolveElements(List elementKeys) {
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