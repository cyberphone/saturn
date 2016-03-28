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

// PaymentRequest object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties = require('./BaseProperties');
const Currencies = require('./Currencies');
const Software = require('./Software');
const Payee = require('./Payee');

function PaymentRequest(rd) {
  this.root = rd;
  this.payee = new Payee(rd.getObject(BaseProperties.PAYEE_JSON));
  this.currency = new Currencies(rd.getString(BaseProperties.CURRENCY_JSON));
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
  
PaymentRequest.SOFTWARE_NAME    = 'WebPKI.org - Merchant';
PaymentRequest.SOFTWARE_VERSION = '1.00';
  
PaymentRequest.prototype.getRequestHash = function() {
  return RequestHash.getRequestHash(root);
};

PaymentRequest.encode = function(payee,
                                 amount,
                                 currency,
                                 referenceId,
                                 expires,
                                 signer) {
  return new JsonUtil.ObjectWriter()
    .setObject(BaseProperties.PAYEE_JSON, payee.writeObject())
    .setBigDecimal(BaseProperties.AMOUNT_JSON, amount, currency.getDecimals())
    .setString(BaseProperties.CURRENCY_JSON, currency.toString())
    .setString(BaseProperties.REFERENCE_ID_JSON, referenceId)
    .setDateTime(BaseProperties.TIME_STAMP_JSON, new Date())
    .setDateTime(BaseProperties.EXPIRES_JSON, expires)
    .setObject(BaseProperties.SOFTWARE_JSON,
               Software.encode(PaymentRequest.SOFTWARE_NAME, PaymentRequest.SOFTWARE_VERSION))
    .setSignature(signer);
};


module.exports = PaymentRequest;
