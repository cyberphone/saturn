/*
 *  Copyright 2015-2020 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

'use strict';

// Saturn "AuthorizationResponse" object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties       = require('./BaseProperties');
const Messages             = require('./Messages');
const Software             = require('./Software');
const AuthorizationRequest = require('./AuthorizationRequest');
    
function AuthorizationResponse(rd) {
  this.root = Messages.parseBaseMessage(Messages.AUTHORIZATION_RESPONSE, rd);
  this.authorizationRequest = new AuthorizationRequest(Messages.getEmbeddedMessage(Messages.AUTHORIZATION_REQUEST, rd));
  this.accountReference = rd.getString(BaseProperties.ACCOUNT_REFERENCE_JSON);
  this.encryptedAccountData = rd.getObject(BaseProperties.ENCRYPTED_ACCOUNT_DATA_JSON).getEncryptionObject().require(true);
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  this.optionalLogData = rd.getStringConditional(BaseProperties.LOG_DATA_JSON);
  this.dateTime = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  this.signatureDecoder = rd.getSignature(BaseProperties.AUTHORIZATION_SIGNATURE_JSON);
  this.signatureDecoder.getCertificatePath();
  rd.checkForUnread();
}

AuthorizationResponse.prototype.getReferenceId = function() {
  return this.referenceId;
}

AuthorizationResponse.prototype.getAccountReference = function() {
  return this.accountReference;
}

AuthorizationResponse.prototype.getSignatureDecoder = function() {
  return this.signatureDecoder;
}

AuthorizationResponse.prototype.getAuthorizationRequest = function() {
  return this.authorizationRequest;
}

module.exports = AuthorizationResponse;
