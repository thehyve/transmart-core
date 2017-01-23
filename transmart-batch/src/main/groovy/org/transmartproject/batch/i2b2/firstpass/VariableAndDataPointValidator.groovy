package org.transmartproject.batch.i2b2.firstpass

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import org.transmartproject.batch.i2b2.codelookup.CodeLookupStore
import org.transmartproject.batch.i2b2.fact.FactDataType
import org.transmartproject.batch.i2b2.fact.FactFactory
import org.transmartproject.batch.i2b2.fact.FactValue
import org.transmartproject.batch.i2b2.mapping.I2b2MappingEntry
import org.transmartproject.batch.i2b2.misc.DateConverter
import org.transmartproject.batch.i2b2.variable.*
import org.transmartproject.batch.support.ConfigurableLengthSemanticsTrait
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.ext.DefaultHandler2

import javax.xml.XMLConstants
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

import static org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable.I2b2DimensionVariableType.*

/**
 * Validates facts on the first pass.
 */
@Component
@JobScope
@CompileStatic
@Slf4j
class VariableAndDataPointValidator implements Validator, ConfigurableLengthSemanticsTrait {

    private static final int MAX_SUPPORTED_SCALE = 5
    private static final int MAX_SUPPORTED_MAGNITUDE = 18 - 5
    private static final int MAX_SUPPORTED_INTEGER_PRECISION = 38
    private static final int MAX_LENGTH_OF_TEXT = 255

    private final static SAXParserFactory FACTORY = SAXParserFactory.newInstance().with {
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        it
    }

    @Autowired
    private FactFactory factFactory

    @Autowired
    private CodeLookupStore codeLookupStore

    @Autowired
    private DateConverter dateConverter

    private final Set<DimensionI2b2Variable> hadMissingEnumValuesWarningIssued = []

    @Override
    boolean supports(Class<?> clazz) {
        clazz == I2b2FirstPassDataPoint
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP) // for multi-dispatch
    void validate(Object target, Errors errors) {
        I2b2FirstPassDataPoint dataPoint = (I2b2FirstPassDataPoint) target

        if (dataPoint.entry.mandatory && dataPoint.data == null) {
            errors.reject 'columnIsMandatory',
                    [dataPoint.entry.columnNumber, dataPoint.entry.filename] as Object[],
                    null
            return
        }
        if (dataPoint.data == null) {
            return
        }

        // multi-dispatch
        doValidate(dataPoint.entry.i2b2Variable,
                dataPoint.entry,
                dataPoint.data,
                errors)
    }

    @SuppressWarnings('ConstantAssertExpression')
    private void doValidate(DimensionI2b2Variable var,
                            I2b2MappingEntry entry,
                            String data,
                            Errors errors) {
        switch (var.variableType) {
            case DATE:
                validateDate(entry, data, errors)
                break

            case STRING:
                Integer maxSize = (Integer) var.parameters['maxSize']
                if (maxSize) {
                    int length = lengthOf(data)
                    if (length > maxSize) {
                        errors.reject 'dataFileStringTooLarge',
                                [entry.columnNumber, entry.filename,
                                 length, maxSize] as Object[], null
                    }
                }
                break

            case INTEGER:
                BigInteger bi
                try {
                    bi = new BigInteger(data)
                } catch (NumberFormatException nfe) {
                    errors.reject 'invalidBigInteger',
                            [entry.columnNumber, entry.filename] as Object[], null
                    break
                }

                Integer minValue = (Integer) var.parameters['minValue']
                if (bi < minValue) {
                    errors.reject 'integerTooSmall',
                            [entry.columnNumber, entry.filename, minValue] as Object[],
                            null
                }

                int precision = new BigDecimal(bi).precision()
                if (precision > MAX_SUPPORTED_INTEGER_PRECISION) {
                    errors.reject 'integerPrecisionTooLarge',
                            [entry.columnNumber, entry.filename,
                             precision, MAX_SUPPORTED_INTEGER_PRECISION] as Object[],
                            null
                }
                break

            case ENUMERATION:
                List<String> values = (List<String>) var.parameters['values']
                assert values != null
                if (!(data in values)) {
                    errors.reject 'invalidEnumerationVariableValue',
                            [entry.columnNumber, entry.filename, values] as Object[],
                            null
                }
                break

            case ENUMERATION_LOOKUP:
                Set<String> values = codeLookupStore.getCodesFor(
                        var.dimensionTable, var.dimensionColumn)
                if (values.empty) {
                    if (!hadMissingEnumValuesWarningIssued.contains(var)) {
                        hadMissingEnumValuesWarningIssued << var
                        log.warn("Could not find enumeration values for " +
                                "table $var.dimensionTable, column " +
                                "$var.dimensionColumn; skipping validation")
                        return
                    }
                }

                if (!(data in values)) {
                    errors.reject 'invalidEnumerationVariableValue',
                            [entry.columnNumber, entry.filename, values] as Object[],
                            null
                }
                break

            default:
                assert false: 'should not get here'
        }
    }

    private void doValidate(DimensionExternalIdI2b2Variable var,
                            I2b2MappingEntry entry,
                            String data,
                            Errors errors) {
        int length = lengthOf(data)
        if (length > var.maxSize) {
            errors.reject 'dataFileStringTooLarge',
                    [entry.columnNumber, entry.filename,
                     length, var.maxSize] as Object[], null
        }
    }

    @SuppressWarnings('UnusedPrivateMethodParameter')
    private void doValidate(ObservationDateI2b2Variable var,
                            I2b2MappingEntry entry,
                            String data,
                            Errors errors) {
        validateDate(entry, data, errors)
    }

    private void validateDate(I2b2MappingEntry entry,
                              String data,
                              Errors errors) {
        try {
            dateConverter.parse(data)
        } catch (IllegalArgumentException e) {
            if (dateConverter.customFormat) {
                errors.reject 'malformedDateCustom',
                        [entry.columnNumber, entry.filename,
                         dateConverter.dateFormat] as Object[], null
            } else {
                errors.reject 'malformedDateISO',
                        [entry.columnNumber, entry.filename] as Object[],
                        null
            }
        }
    }

    @SuppressWarnings('UnusedPrivateMethodParameter')
    private void doValidate(ModifierI2b2Variable var,
                            I2b2MappingEntry entry,
                            String data,
                            Errors errors) {
        doFactVariable(entry, data, errors)
    }

    @SuppressWarnings('UnusedPrivateMethodParameter')
    private void doValidate(ConceptI2b2Variable var,
                            I2b2MappingEntry entry,
                            String data,
                            Errors errors) {
        doFactVariable(entry, data, errors)
    }

    @SuppressWarnings('ReturnNullFromCatchBlock')
    @SuppressWarnings('EmptyIfStatement')
    private void doFactVariable(I2b2MappingEntry entry,
                                String data,
                                Errors errors) {
        FactValue factValue
        try {
            factValue = factFactory.create(entry.dataType, data)
        } catch (IllegalArgumentException | NumberFormatException e) {
            errors.reject 'malformedFactValue',
                    [entry.columnNumber, entry.filename, e.message] as Object[],
                    null
            return
        }

        if (entry.dataType == FactDataType.NUMBER) {
            // postgresql stores numbers in numeric(18,5)
            BigDecimal decValue = factValue.numberValue
            if (decValue.scale() > MAX_SUPPORTED_SCALE) {
                errors.reject 'scaleTooLarge',
                        [entry.columnNumber, entry.filename,
                         decValue.scale(), MAX_SUPPORTED_SCALE] as Object[],
                        null
            }

            // the number will be converted to one of scale 5, so we
            // have to test the magnitude instead of simply checking if
            // the precision is larger than the database-specified 18
            int magnitude = decValue.precision() - decValue.scale()
            if (magnitude > MAX_SUPPORTED_MAGNITUDE) {
                errors.reject 'magnitudeTooLarge',
                        [entry.columnNumber, entry.filename,
                         magnitude, MAX_SUPPORTED_MAGNITUDE] as Object[],
                        null
            }
        } else if (entry.dataType == FactDataType.TEXT) {
            int length = lengthOf(factValue.textValue)
            if (length > MAX_LENGTH_OF_TEXT) {
                errors.reject 'textValueTooLong',
                        [entry.columnNumber, entry.filename,
                         length, MAX_LENGTH_OF_TEXT] as Object[],
                        null
            }
        } else if (entry.dataType == FactDataType.NLP) {
            // should be at the very least well formed xml
            SAXParser parser = FACTORY.newSAXParser()
            try {
                def inputSource = new InputSource(new StringReader(factValue.blob))
                parser.parse(inputSource, new DefaultHandler2())
            } catch (SAXException sex) {
                errors.reject 'xmlNotWellFormed', [
                        entry.columnNumber, entry.filename,
                        sex.message, factValue.blob] as Object[], null
            }
        } else if (entry.dataType == FactDataType.BLOB) {
            // all allowed
        } else {
            // should never happen
            throw new IllegalArgumentException("Unknown type: ${entry.dataType}")
        }
    }
}
