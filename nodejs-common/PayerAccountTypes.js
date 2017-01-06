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

// Saturn "PayerAccountTypes" object

const ACCOUNT_TYPES = [
  ['SUPER_CARD',   true,  'https://supercard.com',   'SuperCard'], 
  ['BANK_DIRECT',  false, 'https://bankdirect.net',  'Bank Direct'],
  ['UNUSUAL_CARD', false, 'https://unusualcard.com', 'UnusualCard']
];

const toAccountType = new Map();

function PayerAccountTypes(cardPayment, typeUri, commonName) {
  this.cardPayment = cardPayment;
  this.typeUri = typeUri;
  this.commonName = commonName;
}

PayerAccountTypes.prototype.isCardPayment = function() {
  return this.cardPayment;
};

PayerAccountTypes.prototype.getTypeUri = function() {
  return this.typeUri;
};

PayerAccountTypes.prototype.getCommonName = function() {
  return this.commonName;
};

PayerAccountTypes.fromTypeUri = function(typeUri) {
  var payerAccountType = toAccountType.get(typeUri);
  if (payerAccountType === undefined) {
    throw new TypeError('No such account type: ' + typeUri);
  }
  return payerAccountType;
};

ACCOUNT_TYPES.forEach((entry) => {
  var payerAccountType = new PayerAccountTypes(entry[1], entry[2], entry[3]);
  toAccountType.set(entry[2], payerAccountType)
  PayerAccountTypes[entry[0]] = payerAccountType;
});

module.exports = PayerAccountTypes;
