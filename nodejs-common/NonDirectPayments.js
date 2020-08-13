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

//Saturn "NonDirectPayments" object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties = require('./BaseProperties');

const NON_DIRECT_PAYMENTS = ['GAS_STATION', 'BOOKING', 'DEPOSIT', 'REOCCURRING', 'OTHER'];

let NonDirectPayments = {

  fromType: function(type) {
    let q = 0;
    while (q < NON_DIRECT_PAYMENTS.length) {
      let entry = NON_DIRECT_PAYMENTS[q++];
      if (entry == type) {
        return entry;
      }
    };
    throw new TypeError('No such type: ' + type);
  }

};

NON_DIRECT_PAYMENTS.forEach((entry) => {
  NonDirectPayments[entry] = entry;
});

module.exports = NonDirectPayments;
