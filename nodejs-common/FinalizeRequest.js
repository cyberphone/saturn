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

// FinalizeRequest object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties = require('./BaseProperties');
const Messages = require('./Messages');
const Software = require('./Software');
const ReserveOrDebitResponse = require('./ReserveOrDebitResponse');
const ReserveOrDebitRequest = require('./ReserveOrDebitRequest');

function FinalizeRequest(rd) {
  this.root = Messages.parseBaseMessage(Messages.FINALIZE_REQUEST, rd);
  this.embeddedResponse = new ReserveOrDebitResponse(rd.getObject(BaseProperties.PROVIDER_AUTHORIZATION_JSON));
  this.amount = rd.getBigDecimal(BaseProperties.AMOUNT_JSON,
                                 this.embeddedResponse.getPaymentRequest().getCurrency().getDecimals());
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  this.timeStamp = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.software = new Software(rd);
  var outerPublicKey = rd.getSignature().getPublicKey();
  this.paymentRequest = this.embeddedResponse.getPaymentRequest();
  ReserveOrDebitRequest.comparePublicKeys(outerPublicKey, this.paymentRequest);
  if (this.amount.cmp(this.paymentRequest.getAmount()) > 0) {
    throw new TypeError('Final amount must be less or equal to reserved amount');
  }
  rd.checkForUnread();
}

FinalizeRequest.prototype.getEmbeddedResponse = function() {
  return this.embeddedResponse;
};

FinalizeRequest.prototype.getAmount = function() {
  return this.amount;
};
  
FinalizeRequest.prototype.getReferenceId = function() {
  return this.referenceId;
};

FinalizeRequest.encode = function(providerResponse,
                                  amount,  // Less or equal the reserved amount
                                  referenceId,
                                  signer) {
  return Messages.createBaseMessage(Messages.FINALIZE_REQUEST)
    .setBigDecimal(BaseProperties.AMOUNT_JSON,
                   amount,
                   providerResponse.getPaymentRequest().getCurrency().getDecimals())
    .setObject(BaseProperties.PROVIDER_AUTHORIZATION_JSON, providerResponse.root)
    .setString(BaseProperties.REFERENCE_ID_JSON, referenceId)
    .setDateTime(BaseProperties.TIME_STAMP_JSON, new Date(), true)
    .setObject(BaseProperties.SOFTWARE_JSON, 
               Software.encode (PaymentRequest.SOFTWARE_NAME,
                                PaymentRequest.SOFTWARE_VERSION))
    .setSignature(signer);
};

module.exports = FinalizeRequest;
