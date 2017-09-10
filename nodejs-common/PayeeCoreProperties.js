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

// Saturn "PayeeCoreProperties" object


const JsonUtil  = require('webpki.org').JsonUtil;
const Jcs       = require('webpki.org').Jcs;

const BaseProperties        = require('./BaseProperties');

function PayeeCoreProperties(rd) {
  this.homePage = rd.getString(BaseProperties.HOME_PAGE_JSON);
  this.commonName = rd.getString(BaseProperties.COMMON_NAME_JSON);
  this.id = rd.getString(BaseProperties.ID_JSON);
  this.signatureParameters = [];
  var paramArray = rd.getArray(BaseProperties.SIGNATURE_PARAMETERS_JSON);
  do {
    var entry = paramArray.getObject();
    var param = {};
    param[Jcs.ALGORITHM_JSON] = entry.getString(Jcs.ALGORITHM_JSON);
    param[Jcs.PUBLIC_KEY_JSON] = entry.getPublicKey();
    this.signatureParameters.push(param);
  } while (paramArray.hasMore());
  rd.checkForUnread();
}

PayeeCoreProperties.prototype.getTimeStamp = function() {
  return this.timeStamp;
};

PayeeCoreProperties.prototype.getReferenceId = function() {
  return this.referenceId;
};

PayeeCoreProperties.prototype.getAmount = function() {
  return this.actualAmount;
};

PayeeCoreProperties.prototype.getTestMode = function() {
  return this.authorizationResponse.authorizationRequest.testMode;
};

PayeeCoreProperties.prototype.getPayee = function() {
  return this.authorizationResponse.authorizationRequest.paymentRequest.payee;
};

PayeeCoreProperties.prototype.getPublicKey = function() {
  return this.publicKey;
};

PayeeCoreProperties.prototype.getAuthorizationResponse = function() {
  return this.authorizationResponse;
};

PayeeCoreProperties.prototype.getPaymentRequest = function() {
  return this.authorizationResponse.authorizationRequest.paymentRequest;
};

PayeeCoreProperties.prototype.verifyPayerProvider = function(paymentRoot) {
  this.authorizationResponse.signatureDecoder.verifyTrust(paymentRoot);
};

PayeeCoreProperties.prototype.getProtectedAccountData = function(decryptionKeys) {
  return new ProtectedAccountData(JsonUtil.ObjectReader.parse(
                                      this.authorizationResponse.encryptedAccountData
                                          .getDecryptedData(decryptionKeys)),
                                  this.authorizationResponse.authorizationRequest.payerAccountType);
};


module.exports = PayeeCoreProperties;
