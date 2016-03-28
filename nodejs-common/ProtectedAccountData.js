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

const BaseProperties = require('./BaseProperties');
const AccountDescriptor = require('./AccountDescriptor');

function ProtectedAccountData(rd) {
  this.root = rd;
  this.accountDescriptor = new AccountDescriptor(rd.getObject(BaseProperties.PAYER_ACCOUNT_JSON));
  this.accountHolder = rd.getString(BaseProperties.ACCOUNT_HOLDER_JSON);
  this.expires = rd.getDateTime(BaseProperties.EXPIRES_JSON);
  this.accountSecurityCode = rd.getString(BaseProperties.ACCOUNT_SECURITY_CODE_JSON);
  rd.checkForUnread();
}

ProtectedAccountData.prototype.getAccountDescriptor = function() {
  return this.accountDescriptor;
};

ProtectedAccountData.prototype.getAccountHolder = function() {
  return this.accountHolder;
};

ProtectedAccountData.prototype.getExpires = function() {
  return this.expires;
};

ProtectedAccountData.prototype.getAccountSecurityCode = function() {
  return this.accountSecurityCode;
};
 
ProtectedAccountData.prototype.toString = function() {
    return this.root.toString();
};

ProtectedAccountData.encode = function(accountDescriptor,
                                       accountHolder,
                                       expires,
                                       accountSecurityCode) {
  return new JsonUtil.ObjectWriter()
    .setObject(BaseProperties.PAYER_ACCOUNT_JSON, accountDescriptor.writeObject())
    .setString(BaseProperties.ACCOUNT_HOLDER_JSON, accountHolder)
    .setDateTime(BaseProperties.EXPIRES_JSON, expires)
    .setString(BaseProperties.ACCOUNT_SECURITY_CODE_JSON, accountSecurityCode);
};


module.exports = ProtectedAccountData;
  
