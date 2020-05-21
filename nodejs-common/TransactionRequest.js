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

// Saturn "TransactionRequest" object

const JsonUtil  = require('webpki.org').JsonUtil;
const ByteArray = require('webpki.org').ByteArray;

const BaseProperties        = require('./BaseProperties');
const Messages              = require('./Messages');
const Software              = require('./Software');
const AuthorizationResponse = require('./AuthorizationResponse');
const AuthorizationRequest  = require('./AuthorizationRequest');
const AccountDataDecoder    = require('./AccountDataDecoder');

function TransactionRequest(rd) {
  this.root = Messages.parseBaseMessage(Messages.TRANSACTION_REQUEST, rd);
  this.authorizationResponse = new AuthorizationResponse(Messages.getEmbeddedMessage(Messages.AUTHORIZATION_RESPONSE, rd));
  this.recipientUrl = rd.getString(BaseProperties.RECIPIENT_URL_JSON);
  this.actualAmount = rd.getBigDecimal(BaseProperties.AMOUNT_JSON,
                                       this.authorizationResponse.authorizationRequest.paymentRequest.currency.decimals);
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  this.timeStamp = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  this.publicKey = rd.getSignature(BaseProperties.REQUEST_SIGNATURE_JSON).getPublicKey();
  rd.checkForUnread();
}

TransactionRequest.prototype.getTimeStamp = function() {
  return this.timeStamp;
};

TransactionRequest.prototype.getReferenceId = function() {
  return this.referenceId;
};

TransactionRequest.prototype.getAmount = function() {
  return this.actualAmount;
};

TransactionRequest.prototype.getTestMode = function() {
  return this.authorizationResponse.authorizationRequest.testMode;
};

TransactionRequest.prototype.getPayeeAuthorityUrl = function() {
  return this.authorizationResponse.authorizationRequest.payeeAuthorityUrl;
};

TransactionRequest.prototype.getPublicKey = function() {
  return this.publicKey;
};

TransactionRequest.prototype.getAuthorizationPublicKey = function() {
  return this.authorizationResponse.authorizationRequest.publicKey;
};

TransactionRequest.prototype.getAuthorizationResponse = function() {
  return this.authorizationResponse;
};

TransactionRequest.prototype.getPaymentRequest = function() {
  return this.authorizationResponse.authorizationRequest.paymentRequest;
};

TransactionRequest.prototype.verifyPayerProvider = function(paymentRoot) {
  this.authorizationResponse.signatureDecoder.verifyTrust(paymentRoot);
};

TransactionRequest.prototype.getProtectedCardData = function(decryptionKeys) {
  let cardData = new AccountDataDecoder(JsonUtil.ObjectReader.parse(
                               this.authorizationResponse.encryptedAccountData
                                          .getDecryptedData(decryptionKeys)));
  cardData.checkAccountTypes(AccountDataDecoder.SUPERCARD_ACCOUNT);
  return cardData;
};

module.exports = TransactionRequest;
