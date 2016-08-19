/**
 * FormValidator.js
 * User: rnugraha
 * Date: 29-10-13
 * Time: 15:27
 */

// Default error messages
var defaults = {
    messages: {
        required: 'The {0} field is required.',
        matches: 'The {0} field does not match the {1} field.',
        "default": 'The {0} field is still set to default, please change.',
        valid_email: 'The {0} field must contain a valid email address.',
        valid_emails: 'The {0} field must contain all valid email addresses.',
        min_length: 'The {0} field must be at least {1} characters in length.',
        max_length: 'The {0} field must not exceed {1} characters in length.',
        exact_length: 'The {0} field must be exactly {1} characters in length.',
        greater_than: 'The {0} field must contain a number greater than {1}.',
        less_than: 'The {0} field must contain a number less than {1}.',
        alpha: 'The {0} field must only contain alphabetical characters.',
        alpha_numeric: 'The {0} field must only contain alpha-numeric characters.',
        alpha_dash: 'The {0} field must only contain alpha-numeric characters, underscores, and dashes.',
        numeric: 'The {0} field must contain only numbers.',
        integer: 'The {0} field must contain an integer.',
        integer_range: '{0} must be between {1} and {2}.',
        integer_min: '{0} must be greater than or equal to {1}.',
        integer_max: '{0} must be less than or equal to {1}.',
        decimal: 'The {0} field must contain a decimal number.',
        is_natural: 'The {0} field must contain only positive numbers.',
        is_natural_no_zero: 'The {0} field must contain a number greater than zero.',
        valid_ip: 'The {0} field must contain a valid IP.',
        valid_base64: 'The {0} field must contain a base64 string.',
        valid_credit_card: 'The {0} field must contain a valid credit card number.',
        is_file_type: 'The {0} field must contain only {1} files.',
        valid_url: 'The {0} field must contain a valid URL.',
        valid_high_dimensional_node: 'The {0} field must contain high dimensional node.',
        valid_high_dimensional_type: 'The {0} field contains invalid high dimensional data type.',
        no_high_dimensional_type: 'Unknown high dimensional data. Please verify first the high dimensional data ' +
            'in field {0}.',
        min_two_nodes: 'The {0} must be more than one nodes.',
        min_two_subsets: 'Marker Selection requires two subsets of cohorts to be selected. Please use the Comparison ' +
            'Tab and select the cohorts',
        identical_elements: 'The {0} are not all the same',
        missing_elements: 'No matching {0} found'
    },
    callback: function(errors) {

    }
};

// Some basic RegExp
var ruleRegex = /^(.+?)\[(.+)\]$/,
    numericRegex = /^[0-9]+$/,
    integerRegex = /^\-?[0-9]+$/,
    decimalRegex = /^\-?[0-9]*\.?[0-9]+$/,
    emailRegex = /^[a-zA-Z0-9.!#$%&amp;'*+\-\/=?\^_`{|}~\-]+@[a-zA-Z0-9\-]+(?:\.[a-zA-Z0-9\-]+)*$/,
    alphaRegex = /^[a-z]+$/i,
    alphaNumericRegex = /^[a-z0-9]+$/i,
    alphaDashRegex = /^[a-z0-9_\-]+$/i,
    naturalRegex = /^[0-9]+$/i,
    naturalNoZeroRegex = /^[1-9][0-9]*$/i,
    ipRegex = /^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[0-9]{1,2})\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[0-9]{1,2})$/i,
    base64Regex = /[^a-zA-Z0-9\/\+=]/i,
    numericDashRegex = /^[\d\-\s]+$/,
    urlRegex = /^((http|https):\/\/(\w+:{0,1}\w*@)?(\S+)|)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?$/;


var FormValidator = function (inputArray) {
    this.inputs = inputArray;
    this.error_messages = [];
}

FormValidator.prototype.validateInputForm = function () {

    var _isValid = true; //init
    var _inputArr = this.inputs;

    for (var i=0; i < _inputArr.length; i++) { // loop through input array

        // single instance of input object
        var _el = _inputArr[i].el;
        var _value = _inputArr[i].value;
        var _label = _inputArr[i].label;
        var _validations = _inputArr[i].validations;

        // initialize
        var _isRequired = false;
        var _isInteger = false;
        var _isDecimal = false;
        var _isHighDimensional = false;
        var _isTwoSubsets = false;

        for (var j = 0; j < _validations.length; j++) { // loop into validations for an input

            // 1st level validation - check mandatory
            if (_validations[j].type == "REQUIRED") {
                _isRequired = this.required(_el, _label);
                _isValid = _isValid && _isRequired;
            }

            if (_isValid) {
                switch (_validations[j].type)
                {
                    case 'INTEGER' :
                        if (_el.value !== null && _el.value !== '') {
                            _isInteger = this.valid_integer(_el, _label, _validations[j]);
                            _isValid = _isValid && _isInteger;
                        }
                        break;
                    case 'DECIMAL' :
                        if (_el.value !== null && _el.value !== '') {
                            _isDecimal = this.valid_decimal(_el, _label, _validations[j]);
                            _isValid = _isValid && _isDecimal;
                        }
                        break;
                    case 'HIGH_DIMENSIONAL' :
                        _isHighDimensional =  this.valid_high_dimensional(_el, _label, _validations[j]);
                        _isValid = _isValid && _isHighDimensional;
                        break;
                    case 'HIGH_DIMENSIONAL_ACGH' :
                        _isHighDimensional =  this.valid_high_dimensional_acgh(_el, _label, _validations[j]);
                        _isValid = _isValid && _isHighDimensional;
                        break;
                    case 'GROUP_VARIABLE' :
                        _isHighDimensional =  this.valid_group_variable(_el, _label);
                        _isValid = _isValid && _isHighDimensional;
                        break;
                    case 'MIN_TWO_SUBSETS' :
                        _isTwoSubsets = this.min_two_subsets(_value, _label);
                        _isValid = _isValid && _isTwoSubsets;
                        break;
                    case 'IDENTICAL_ITEMS' :
                        _areIdentical = this.identical_elements(_el, _label);
                        _isValid = _isValid && _areIdentical;
                        break;
                }
            }
        } // end validation loop
    }

    return _isValid;
}

// General validations
// -------------------------------------------------

FormValidator.prototype.required = function (el, label) {

    var retVal = true;

    if (el instanceof Ext.Element) {  // if input element is instance of Ext JS
        retVal = el.dom.children.length > 0 ? true : false;
    } else {
        var value = el.value;
        retVal = (value !== null && value !== '');
    }

    if (!retVal) {
        this.push_error(defaults.messages.required, [label]);
    }

    return retVal;

}

FormValidator.prototype.valid_range = function (el, label, validator) {
    var retVal = true;
    if (typeof validator.min !== 'undefined' && typeof validator.max !== 'undefined') {
        var isWithinRange = (el.value >= validator.min) && (el.value <= validator.max) ? true : false;
        retVal = retVal && isWithinRange;
        if (!retVal)  this.push_error(defaults.messages.integer_range, [label, validator.min, validator.max]);
    } else if (typeof validator.min !== 'undefined' && typeof validator.max === 'undefined') {
        var isGreaterThanEqual = (el.value >= validator.min) ? true : false;
        retVal = retVal && isGreaterThanEqual;
        if (!retVal)  this.push_error(defaults.messages.integer_min, [label, validator.min]);
    } else if (typeof validator.min === 'undefined' && typeof validator.max !== 'undefined') {
        var isLessThanEqual = (el.value <= validator.min) ? true : false;
        retVal = retVal && isLessThanEqual;
        if (!retVal)  this.push_error(defaults.messages.integer_max, [label, validator.max]);
    }
    return retVal;
}

FormValidator.prototype.valid_integer = function (el, label, validator) {

    var retVal = integerRegex.test(el.value);

    if (!retVal) {
        this.push_error(defaults.messages.integer, [label]);
    } else {
        retVal = retVal && this.valid_range(el, label, validator);
    }
    return retVal;
}

FormValidator.prototype.valid_decimal = function (el, label, validator) {

    var retVal = decimalRegex.test(el.value);

    if (!retVal) {
        this.push_error(defaults.messages.decimal, [label]);
    } else {
        retVal = retVal && this.valid_range(el, label, validator);
    }
    return retVal;
}

// Custom validations
// -------------------------------------------------

FormValidator.prototype.identical_elements = function (el, label) {

  var returnValue = true;

  var onlyUnique = function(value, index) {
    return this.indexOf(value) === index;
  }

  var nUnique = el.filter(onlyUnique, el).length;

  console.log("identical_elements nUnique: "+nUnique);

  if (nUnique < 1) {
    this.push_error(defaults.messages.missing_elements, [label]);
    returnValue = false;
  }

  if (nUnique > 1) {
    this.push_error(defaults.messages.identical_elements, [label]);
    returnValue = false;
  }

  return returnValue;
}

FormValidator.prototype.valid_high_dimensional_acgh = function (el, label, validator) {

    var retVal = true;
    var nodes = el.dom.childNodes;

    for (var i=0; i<nodes.length; i++) { // loop through container
        var strVisualAttributes = nodes[i].attributes['visualattributes'].value;
        var isHD = true;

        // check if node is high dimensional data node
        if (strVisualAttributes.indexOf('HIGH_DIMENSIONAL') == -1) { // if not

            isHD = false;
            retVal = retVal && isHD;
            this.push_error(defaults.messages.valid_high_dimensional_node, [label]);

        }
    }
    return retVal;
}


FormValidator.prototype.valid_group_variable = function (el, label) {

    var retVal = true;
    var nodes = el.dom.childNodes;

    if (nodes.length < 2) {
        this.push_error(defaults.messages.min_two_nodes, [label]);
        return false;
    }

    return retVal;
}


// validate input to receive only high dimensional node
FormValidator.prototype.valid_high_dimensional = function (el, label, validator) {

    var retVal = true;
    var nodes = el.dom.childNodes;

    for (var i=0; i<nodes.length; i++) { // loop through container

        var strVisualAttributes = nodes[i].attributes['visualattributes'].value;
        var isHD = true;

        // check if node is high dimensional data node
        if (strVisualAttributes.indexOf('HIGH_DIMENSIONAL') == -1) { // if not

            isHD = false;
            retVal = retVal && isHD;
            this.push_error(defaults.messages.valid_high_dimensional_node, [label]);

        }
    }
    return retVal;
}

// validate selected high dimensional platform
FormValidator.prototype.valid_high_dimensional_type = function (validator, label) {
    var retVal = false;

    // validate high dimensional platform
    if (!validator.high_dimensional_type) {
        this.push_error(defaults.messages.no_high_dimensional_type, [label]);
    } else {


        for (var key in highDimensionalData.supportedTypes) {
            if (validator.high_dimensional_type == (highDimensionalData.supportedTypes[key]).type) {
                retVal = true;
                break;
            }
        }

        if (!retVal) {
            this.push_error(
                defaults.messages.valid_high_dimensional_type, [label]);
        }
    }

    return retVal;
}

// validate cohort selection must have at least two cohorts
FormValidator.prototype.min_two_subsets = function (subsets, label) {
    // validate high dimensional pathway
    for (var i=0; i<subsets.length; i++) {
        if (!subsets[i]) {
            this.push_error(defaults.messages.min_two_subsets, [label]);
            return false;
        }
    }
    return true;
}

// Utils
// -------------------------------------------------

FormValidator.prototype.format = function (str, arguments)
{
    var replaced = str;
    for (var i = 0; i < arguments.length; i++) {
        var regex = new RegExp('\\{'+i+'\\}');
        replaced = replaced.replace(regex,arguments[i]);
    }
    return replaced;
}

FormValidator.prototype.push_error = function (err_msg, args) {
    var err = this.format(err_msg, args);
    this.error_messages.push(err);
}

FormValidator.prototype.display_errors = function () {

    var _err_msgs = this.error_messages;
    var _err_str = "";

    // concatenate all error messages into a nice HTML friendly string message.
    for (var i=0; i<_err_msgs.length; i++) {
        _err_str = _err_str.concat("&bull; " + _err_msgs[i] + "<br>");
    }

    // Display error messages inside extJs message box
    Ext.MessageBox.show({
        title: 'Validation Error',
        msg: _err_str,
        buttons: Ext.MessageBox.OK,
        icon: Ext.MessageBox.ERROR
    });
}
