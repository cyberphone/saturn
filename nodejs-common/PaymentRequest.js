/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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

// Saturn "PaymentRequest" object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties    = require('./BaseProperties');
const Currencies        = require('./Currencies');
const Software          = require('./Software');
const Payee             = require('./Payee');
const NonDirectPayments = require('./NonDirectPayments');

function PaymentRequest(rd) {
  this.root = rd;
  this.payee = new Payee(rd.getObject(BaseProperties.PAYEE_JSON));
  this.currency = Currencies.valueOf(rd.getString(BaseProperties.CURRENCY_JSON));
  if (rd.hasProperty(BaseProperties.NON_DIRECT_PAYMENT_JSON)) {
    this.nonDirectPayment = NonDirectPayments.fromType(rd.getString(BaseProperties.NON_DIRECT_PAYMENT_JSON));
  }
  this.amount = rd.getBigDecimal(BaseProperties.AMOUNT_JSON, this.currency.getDecimals());
  this.referenceId = rd.getString(BaseProperties.REFERENCE_ID_JSON);
  this.dateTime = rd.getDateTime(BaseProperties.TIME_STAMP_JSON);
  this.expires = rd.getDateTime(BaseProperties.EXPIRES_JSON);
  this.software = new Software(rd);
  this.publicKey = rd.getSignature().getPublicKey();
  rd.checkForUnread();
}

PaymentRequest.prototype.getExpires = function() {
  return this.expires;
};

PaymentRequest.prototype.getPayee = function() {
  return this.payee;
};

PaymentRequest.prototype.getAmount = function() {
  return this.amount;
};

PaymentRequest.prototype.getCurrency = function() {
  return this.currency;
};

PaymentRequest.prototype.getReferenceId = function() {
  return this.referenceId;
};
  
PaymentRequest.prototype.getDateTime = function() {
  return this.dateTime;
};

PaymentRequest.prototype.getSoftware = function() {
  return this.software;
};

PaymentRequest.prototype.getPublicKey = function() {
  return this.publicKey;
};
  
PaymentRequest.prototype.getRequestHash = function() {
  return RequestHash.getRequestHash(root);
};

module.exports = PaymentRequest;
