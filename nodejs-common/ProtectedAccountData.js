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

// ProtectedAccountData object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties   = require('./BaseProperties');
const AccountDescriptor = require('./AccountDescriptor');
const CardSpecificData  = require('./CardSpecificData');

function ProtectedAccountData(rd, payerAccountType) {
  this.root = rd;
  this.account = new AccountDescriptor(rd.getObject(BaseProperties.ACCOUNT_JSON));
  if (payerAccountType.cardPayment) {
      if (payerAccountType.typeUri != this.account.typeUri) {
          throw new TypeError('Payment type mismatch');
      }
      this.cardSpecificData = new CardSpecificData(rd);
  }
  rd.checkForUnread();
}

ProtectedAccountData.prototype.getAccount = function() {
  return this.account;
};

ProtectedAccountData.prototype.getCardSpecificData = function() {
  return this.cardSpecificData;
};

ProtectedAccountData.prototype.toString = function() {
    return this.root.toString();
};

ProtectedAccountData.encode = function(account, cardSpecificData) {
  var wr = new JsonUtil.ObjectWriter().setObject(BaseProperties.PAYER_ACCOUNT_JSON, account.writeObject());
  if (cardSpecificData) {
    cardSpecificData.writeData(wr);
  }
  return wr;
};

module.exports = ProtectedAccountData;
  
