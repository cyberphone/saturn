/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License'),
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 
'use strict';

// Saturn "Currencies" object

const CURRENCIES = [
  ['USD', '$\u200a',      true, 2], 
  ['EUR', '\u20ac\u200a', true, 2],
  ['GBP', '\u00a3\u200a', true, 2]
];

function Currencies(symbol,textual, textFirst, decimals) {
  this.symbol = symbol;
  this.textual = textual;
  this.textFirst = textFirst;
  this.decimals = decimals;
}

Currencies.prototype.getDecimals = function() {
  return this.decimals;
}

Currencies.prototype.getSymbol = function() {
  return this.symbol;
}

Currencies.prototype.amountToDisplayString = function(amount) {
  var amountString = amount.toFixed(this.decimals);
  return this.textFirst ? this.textual + amountString : amountString + this.textual;
};

Currencies.prototype.toString = function() {
  return this;
};

Currencies.valueOf = function(currencySymbol) {
  var currency = Currencies[currencySymbol];
  if (currency === undefined) {
    throw new TypeError('No such currency: ' + currencySymbol);
  }
  return currency;
};

CURRENCIES.forEach((entry) => {
  Currencies[entry[0]] = new Currencies(entry[0], entry[1], entry[2], entry[3]);
});

module.exports = Currencies;
