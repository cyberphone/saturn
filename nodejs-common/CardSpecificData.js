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

// Saturn "CardSpecificData" object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties       = require('./BaseProperties');

function CardSpecificData(rd_or_accountHolder, expirationDate, securityCode) {
  if (rd_or_accountHolder instanceof JsonUtil.ObjectReader) {
    this.accountHolder = rd_or_accountHolder.getString(BaseProperties.ACCOUNT_HOLDER_JSON);
    this.expirationDate = rd_or_accountHolder.getDateTime(BaseProperties.EXPIRES_JSON);
    this.securityCode = rd_or_accountHolder.getString(BaseProperties.ACCOUNT_SECURITY_CODE_JSON);
  } else {
    this.accountHolder = accountHolder;
    this.expirationDate = expirationDate;
    this.securityCode = securityCode;
  }
}

CardSpecificData.prototype.writeData = function(wr) {
  return wr.setString(BaseProperties.ACCOUNT_HOLDER_JSON, this.accountHolder)
           .setDateTime(BaseProperties.EXPIRES_JSON, this.expirationDate)
           .setString(BaseProperties.ACCOUNT_SECURITY_CODE_JSON, this.securityCode);
};

CardSpecificData.prototype.getAccountHolder = function() {
  return this.accountHolder;
};

CardSpecificData.prototype.getExpirationDate = function() {
  return this.expirationDate;
};

CardSpecificData.prototype.getSecurityCode = function() {
  return this.securityCode;
};

module.exports = CardSpecificData;
