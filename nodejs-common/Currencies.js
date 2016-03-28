/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
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

// Currencies used by the Web2Native Bridge PoC

const CURRENCIES = [
  'USD', '$\u200a',      true,  2, 
  'EUR', '\u2009\u20ac', false, 2,
  'GBP', '\u00a3\u200a', true,  2
];

function Currencies(currency) {
  for (var i = 0; i < CURRENCIES.length; i += 4) {
      if (CURRENCIES[i++] == currency) {
          this.currency = currency;
          this.symbol = CURRENCIES[i++];
          this.symbolFirst = CURRENCIES[i++];
          this.decimals = CURRENCIES[i];
          return;
      }
  }
  throw new TypeError('Unknown currency: ' + currency);
}

Currencies.prototype.getDecimals = function() {
  return this.decimals;
}

Currencies.prototype.amountToDisplayString = function(amount) {
  var amountString = amount.toFixed(this.decimals);
  return this.symbolFirst ? this.symbol + amountString : amountString + this.symbol;
};

Currencies.prototype.toString = function() {
  return this.currency;
};

  
module.exports = Currencies;
