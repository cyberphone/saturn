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

// CardData object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties = require('./BaseProperties');
const Messages       = require('./Messages');

const CARD_NUMBER_JSON   = 'cardNumber';
const CARD_HOLDER_JSON   = 'cardHolder';
const SECURITY_CODE_JSON = 'securityCode';

const EXPECTED_CONTEXT   = 'https://supercard.com/saturn/v3#account'

function CardData(rd) {
  this.root = rd;
  if (rd.getString(Messages.CONTEXT_JSON) != EXPECTED_CONTEXT) {
    throw new TypeError('Unexpected card data: ' + rd.toString());
  }
  this.cardNumber     = rd.getString(CARD_NUMBER_JSON);
  this.cardHolder     = rd.getString(CARD_HOLDER_JSON);
  this.expirationDate = rd.getDateTime(BaseProperties.EXPIRES_JSON);
  this.securityCode   = rd.getString(SECURITY_CODE_JSON);
  rd.checkForUnread();
}

CardData.prototype.getCardNumber = function() {
  return this.cardNumber;
};

CardData.prototype.getCardHolder = function() {
  return this.cardHolder;
};

CardData.prototype.toString = function() {
    return this.root.toString();
};

module.exports = CardData;
  
