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

const ReservationSubTypes = require('./ReservationSubTypes');
const BaseProperties      = require('./BaseProperties');

let NonDirectPayments = {

  from: function(rd, timeStamp) {
    switch (this.type = rd.getString(BaseProperties.TYPE_JSON)) {
      case 'RESERVATION':
        this.subType = ReservationSubTypes.from(rd.getString(BaseProperties.SUB_TYPE_JSON));
        this.expires = rd.getDateTime(BaseProperties.EXPIRES_JSON);
        break;
      
      case 'RECURRING':
        throw new TypeError('Not yet implemented: ' + this.type);
        break;

      default:
        throw new TypeError('No such type: ' + this.type);
    }
    this.fixed = rd.getBoolean(BaseProperties.FIXED_JSON);
  }

};

module.exports = NonDirectPayments;
