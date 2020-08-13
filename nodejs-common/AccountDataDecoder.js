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

// A decoder for account data. 

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties = require('./BaseProperties');
const Messages       = require('./Messages');
    
const SEPA_ACCOUNT       = 'https://sepa.payments.org/saturn/v3#account';
const SEPA_IBAN_JSON     = 'iban';

const SUPERCARD_ACCOUNT  = 'https://supercard.com/saturn/v3#account';
const CARD_NUMBER_JSON   = 'cardNumber';    // PAN
const CARD_HOLDER_JSON   = 'cardHolder';    // Name

const BG_ACCOUNT         = 'https://bankgirot.se/saturn/v3#account';
const BG_NUMBER_JSON     = 'bgNumber';

function AccountDataDecoder(rd) {
  let accountType = rd.getString(Messages.CONTEXT_JSON); 
  if (accountType == SEPA_ACCOUNT) {
    this.accountId = rd.getString(SEPA_IBAN_JSON);
  } else if (accountType == SUPERCARD_ACCOUNT) {
    this.accountId = rd.getString(CARD_NUMBER_JSON);
    this.accountOwner = rd.getString(CARD_HOLDER_JSON);
    this.expirationDate = rd.getDateTime(BaseProperties.EXPIRES_JSON);
  } else if (accountType == BG_ACCOUNT) {
    this.accountId = rd.getString(BG_NUMBER_JSON);
  } else {
    throw new TypeError('Unknown account type: ' + accountType);
  }
  let nonce = rd.getStringConditional(BaseProperties.NONCE_JSON);
  if (nonce) {
    this.nonce = rd.getBinary(BaseProperties.NONCE_JSON);
    if (this.nonce.length != 32) {
      throw new TypeError('Nonce <> 32 bytes');
    }
  }
  this.accountType = accountType;
  rd.checkForUnread();
}

AccountDataDecoder.prototype.getAccountId = function() {
  return this.accountId;
};

AccountDataDecoder.prototype.getAccountOwner = function() {
  return this.accountOwner;
};

AccountDataDecoder.prototype.getExpirationDate = function() {
  return this.expirationDate;
};

AccountDataDecoder.prototype.getAccountType = function() {
  return this.accountType;
};

AccountDataDecoder.prototype.checkAccountTypes = function(recognized) {
  if (!Array.isArray(recognized)) {
    let temp = recognized;
    recognized = [];
    recognized.push(temp);
  } 
  for (let q = 0; q < recognized.length; q++) {
    if (recognized[q] == this.accountType) {
      return;
    }
  }
  throw new TypeError('Unexpected account type: ' + accountType);
};

module.exports = AccountDataDecoder;

module.exports.SEPA_ACCOUNT      = SEPA_ACCOUNT;
module.exports.SUPERCARD_ACCOUNT = SUPERCARD_ACCOUNT;
module.exports.BG_ACCOUNT        = BG_ACCOUNT;
