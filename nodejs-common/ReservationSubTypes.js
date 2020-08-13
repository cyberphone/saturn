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

//Saturn "ReverationSubTypes" object

const BaseProperties = require('./BaseProperties');

const SUB_TYPES = ['GAS_STATION',
                   'CONSUMABLE',    // Same as GAS_STATION but unspecified
                   'BOOKING',
                   'DEPOSIT'];

let ReservationSubTypes = {

  from: function(subTypeName) {
    for (let q = 0; q < SUB_TYPES.length; q++) {
      if (subTypeName == SUB_TYPES[q]) {
        return subTypeName;
      }
    }
    throw new TypeError('No such sub type: ' + subTypeName);
  }

};

module.exports = ReservationSubTypes;
