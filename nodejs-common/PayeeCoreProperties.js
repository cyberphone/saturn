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

// Saturn "PayeeCoreProperties" object


const JsonUtil  = require('webpki.org').JsonUtil;
const Jsf       = require('webpki.org').Jsf;

const BaseProperties        = require('./BaseProperties');
const Messages              = require('./Messages');

const PAYEE_ACCOUNTS_JSON = 'payeeAccounts';

const SEPA_METHOD          = 'https://sepa.payments.org/saturn/v3#bpd';
const SEPA_PAYEE_IBAN_JSON = 'payeeIban';

function PayeeCoreProperties(rd) {
  this[BaseProperties.LOCAL_PAYEE_ID_JSON] = rd.getString(BaseProperties.LOCAL_PAYEE_ID_JSON);
  this[BaseProperties.HOME_PAGE_JSON] = rd.getString(BaseProperties.HOME_PAGE_JSON);
  this[BaseProperties.COMMON_NAME_JSON] = rd.getString(BaseProperties.COMMON_NAME_JSON);

// TODO the following line lacks the URL fix...
  this.urlSafeId = this[BaseProperties.LOCAL_PAYEE_ID_JSON];

// TODO we only understand SEPA...
  var payeeAccounts = rd.getArray(PAYEE_ACCOUNTS_JSON);
  var hashedAccounts = [];
  do {
    var entry = payeeAccounts.getObject();
    if (entry.getString(Messages.CONTEXT_JSON) != SEPA_METHOD) {
      throw new TypeError('Unknown context: ' + jsonReader.getString(Messages.CONTEXT_JSON));
    }
    entry.getString(SEPA_PAYEE_IBAN_JSON);
    var nonce = entry.getStringConditional(BaseProperties.NONCE_JSON);
    if (nonce) {
      if (entry.getBinary(BaseProperties.NONCE_JSON).length != 32) {
        throw new TypeError('Wrong nonce length');
      }
      hashedAccounts.push(nonce);
    }
  } while (payeeAccounts.hasMore());
  if (hashedAccounts.length > 0) {
    this[BaseProperties.HASHED_PAYEE_ACCOUNTS_JSON] = hashedAccounts;
  }
  this.signatureParameters = [];
  var paramArray = rd.getArray(BaseProperties.SIGNATURE_PARAMETERS_JSON);
  do {
    var entry = paramArray.getObject();
    var param = {};
    param[Jsf.ALGORITHM_JSON] = entry.getString(Jsf.ALGORITHM_JSON);
    param[Jsf.PUBLIC_KEY_JSON] = entry.getPublicKey();
    this[BaseProperties.SIGNATURE_PARAMETERS_JSON].push(param);
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
