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

//Holds payer account type

const ACCOUNT_TYPES = [
    true,  'https://supercard.com',   'SuperCard', 
    false, 'https://bankdirect.net',  'Bank Direct',
    false, 'https://unusualcard.com', 'UnusualCard'
];

function PayerAccountTypes (acquirerBased_or_entry, typeUri, commonName) {
  if (typeof acquirerBased_or_entry == 'number') {
    typeUri = ACCOUNT_TYPES[acquirerBased_or_entry + 1];
    commonName = ACCOUNT_TYPES[acquirerBased_or_entry + 2];
    acquirerBased_or_entry = ACCOUNT_TYPES[acquirerBased_or_entry];
  }
  this.acquirerBased = acquirerBased_or_entry;
  this.typeUri = typeUri;
  this.commonName = commonName;
}

PayerAccountTypes.SUPER_CARD   = new PayerAccountTypes(0);
PayerAccountTypes.BANK_DIRECT  = new PayerAccountTypes(3);
PayerAccountTypes.UNUSUAL_CARD = new PayerAccountTypes(6);

PayerAccountTypes.prototype.isAcquirerBased = function() {
  return this.acquirerBased;
};

PayerAccountTypes.prototype.getTypeUri = function() {
  return this.typeUri;
};

PayerAccountTypes.prototype.getCommonName = function() {
  return this.commonName;
};

PayerAccountTypes.fromTypeUri = function(typeUri) {
  for (var i = 0; i < ACCOUNT_TYPES.length; i++) {
    if (ACCOUNT_TYPES[i + 1] == typeUri) {
      return new PayerAccountTypes(i);
    }
  }
  throw new TypeError('No such account type: ' + typeUri);
};


module.exports = PayerAccountTypes;
