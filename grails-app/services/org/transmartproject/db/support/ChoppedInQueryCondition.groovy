package org.transmartproject.db.support

import org.apache.commons.lang.StringEscapeUtils
//import grails.orm.HibernateCriteriaBuilder
import grails.gorm.CriteriaBuilder
import org.transmartproject.core.exceptions.InvalidRequestException

/**
 * This class aimed to overcome oracle limitation on the number of items in IN clause:
 * Message: ORA-01795: maximum number of expressions in a list is 1000
 *
 * This class constructs composite condition
 * Instead of making one condition
 * patient_num IN (1, 2, 3, ... 1200)
 * it chops list of items and join sub-conditions with OR operations
 * patient_num IN (1, 2, 3, ... 1000) OR patient_num IN (1001, 1002, 1003, ... 1200)
 */
class ChoppedInQueryCondition {

    static final int MAX_LIST_SIZE = 1000
    static final String ALWAYS_TRUE_CONDITION = '1=1'

    private final String fieldName
    private final List inItems

    ChoppedInQueryCondition(String fieldName, List inItems) {
        this.fieldName = fieldName
        this.inItems = inItems
    }

    @Lazy Map parametersValues = {
        if(!inItems) return [:]
        def chunks = inItems.collate(MAX_LIST_SIZE)
        (0..<chunks.size()).collectEntries { index -> ["_${index}".toString(), chunks[index]] }
    }()

    @Lazy String queryConditionTemplate = {
        constructChoppedInQueryCondition { parVals -> ":${parVals.key}"}
    }()

    /**
     * You should prefer using queryConditionTemplate instead.
     */
    @Lazy String populatedQueryCondition = {
        constructChoppedInQueryCondition { parVals ->
            //Note that single quotes always added. Disregard the fact whether value is number or string. AFAIK It's fine with SQL.
            parVals.value.collect{"'${StringEscapeUtils.escapeSql(it.toString())}'"}.join(',')}
    }()

    //HibernateCriteriaBuilder
    void addConstraintsToCriteria(CriteriaBuilder builder) throws InvalidRequestException {
        builder.or {
            parametersValues.collect { parVal ->
                'in' fieldName, parVal.value
            }
        }
    }

    private String constructChoppedInQueryCondition(Closure<String> constructValues) {
        def subCondition = parametersValues.collect { parVal ->
            "${fieldName} IN (${constructValues(parVal)})"}.join(' OR ')
        "(${subCondition ?: ALWAYS_TRUE_CONDITION})".toString()
    }
}
