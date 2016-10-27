package org.transmartproject.db.dataquery2

import groovy.transform.InheritConstructors
import org.apache.commons.lang.NotImplementedException
import org.transmartproject.core.IterableResult
import org.transmartproject.db.clinical.Query
import org.transmartproject.db.i2b2data.Study
import org.transmartproject.db.i2b2data.TrialVisit


abstract class Dimension {
    // TODO(jan): Which properties on dimensions do we actually need for the hypercube to work?

    enum Size {
        SMALL,
        MEDIUM,
        LARGE
    }

    enum Density {
        DENSE,
        SPARSE
    }

    enum Packable {
        PACKABLE    (packable: true),
        NOT_PACKABLE(packable: false);

        boolean packable
    }

    final Size size
    final Density density
    final Packable packable

    Dimension(Size size, Density density, Packable packable) {
        this.size = size
        this.density = density
        this.packable = packable
    }

    IterableResult<Object> getElements(Study[] studies) {
        throw new NotImplementedException()
    }

    def abstract selectIDs(Query query)

    /*
     * This method is assumed to do internal caching. As all implementations call GORM methods, they can rely on the
     * GORM cache.
     * Non-packable dimensions must return a List<Object>
     */
    abstract Collection<Object> resolveElements(Collection<Serializable> elementIds)

    /* This default implementation should be overridden for efficiency for non-packable dimensions.
     */
    Object resolveElement(Serializable elementId) {
        resolveElements([elementId])[0]
    }

}




//TODO: supporting modifier dimensions requires support for sorting, since we need to sort on the full PK except
// modifierCd in order to ensure that modifier ObservationFacts come next to their observation value
class ModifierDimension extends Dimension {
    ModifierDimension(String name, String modifierCode, Size size, Density density, Packable packable) {
        super(size, density, packable)
        this.name = name
        this.modifierCode = modifierCode
    }

    String name
    String modifierCode

    def selectIDs(Query query) {
        query.params.modifierCodes += modifierCode
    }

    @Override
    Collection<Object> resolveElements(Collection<Serializable> elementIds) {
        return elementIds
    }
}

@InheritConstructors
class PatientDimension extends Dimension {

    def selectIDs(Query query) {
        query.criteria.with {
            property 'patient.id'
        }
        query.projectionOwners += this
    }

    @Override
    List<Object> resolveElements(Collection<Serializable> elementIds) {
        org.transmartproject.db.i2b2data.PatientDimension.getAll(elementIds)
    }
}

@InheritConstructors
class ConceptDimension extends Dimension {

    def selectIDs(Query query) {
        query.criteria.with {
            property 'conceptCode'
        }
        query.projectionOwners += this
    }

    @Override
    List<Object> resolveElements(Collection<Serializable> elementIds) {
        org.transmartproject.db.i2b2data.ConceptDimension.getAll(elementIds)
    }
}

@InheritConstructors
class TrialVisitDimension extends Dimension {

    def selectIDs(Query query) {
        query.criteria.with {
            property 'trialVisit.id'
        }
        query.projectionOwners += this
    }

    @Override
    List<Object> resolveElements(Collection<Serializable> elementIds) {
        TrialVisit.getAll(elementIds)
    }
}

@InheritConstructors
class StudyDimension extends Dimension {

    def selectIDs(Query query) {
        query.criteria.with {
            trialVisit {
                property 'study.id'
            }
        }
        query.projectionOwners += this
    }

    Collection<Object> resolveElements(Collection<Serializable> elementIds) {
        Study.getAll(elementIds)
    }
}

@InheritConstructors
class StartTimeDimension extends Dimension {
    def selectIDs(Query query) {
        query.projection += {
            property 'startDate'
        }
        query.projectionOwners += this
    }

    Collection<Object> resolveElements(Collection<Serializable> elementIds) {
        elementIds
    }
}



