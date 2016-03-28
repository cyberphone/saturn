/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 
'use strict';

//For transferring status for which is related to the payer's account

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties = require('./BaseProperties');

function ERRORS(errorCode) {
  this.errorCode = errorCode;
}

const CLEAR_TEXT_ERRORS = [
    'Insufficient Funds',
    'Expired Credential',
    'Expired Fund Reservation',
    'Account is blocked',
    'Not Authorized',
    'Other Error'
];

function ErrorReturn(error_or_reader, optionalDescription) {
  if (error_or_reader instanceof JsonUtil.ObjectReader) {
    var errorCode = error_or_reader.getInt(BaseProperties.ERROR_CODE_JSON);
    optionalDescription = error_or_reader.getStringConditional(BaseProperties.DESCRIPTION_JSON);
    error_or_reader.checkForUnread();
    if (errorCode < 0 || errorCode >= CLEAR_TEXT_ERRORS.length) {
      throw new TypeError('Error code out of range: ' + errorCode);
    }
    error_or_reader = new ERRORS(errorCode);
  } else if (!(error_or_reader instanceof ERRORS)) {
    throw new TypeError('Unexpected argument type');
  }
  this.error = error_or_reader;
  this.optionalDescription = optionalDescription;
}

ErrorReturn.INSUFFICIENT_FUNDS  = new ERRORS(0);
ErrorReturn.EXPIRED_CREDENTIAL  = new ERRORS(1);
ErrorReturn.EXPIRED_RESERVATION = new ERRORS(2);
ErrorReturn.BLOCKED_ACCOUNT     = new ERRORS(3);
ErrorReturn.NOT_AUTHORIZED      = new ERRORS(4);
ErrorReturn.OTHER_ERROR         = new ERRORS(5);
  
ErrorReturn.prototype.getError = function() {
  return this.error;
};

ErrorReturn.prototype.getErrorCode = function() {
  return this.error.errorCode;
};

ErrorReturn.prototype.getClearText = function() {
  return CLEAR_TEXT_ERRORS[this.error.errorCode];
};

ErrorReturn.prototype.getOptionalDescription = function() {
  return this.optionalDescription;
};

ErrorReturn.prototype.write = function(wr) {
  wr.setInt(BaseProperties.ERROR_CODE_JSON, this.error.errorCode);
  return this.optionalDescription == null ? wr : wr.setString(BaseProperties.DESCRIPTION_JSON, this.optionalDescription);
};


module.exports = ErrorReturn;
