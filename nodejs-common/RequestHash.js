/*
 *  Copyright 2015-2018 WebPKI.org (http://webpki.org).
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

// RequestHash object

const Jcs = require('webpki.org').Jcs;
const Hash = require('webpki.org').Hash;

const BaseProperties = require('./BaseProperties');

const RequestHash = {

  JOSE_SHA_256_ALG_ID : 'S256',    // Well, not really JOSE but "similar" :-)

  getRequestHash : function(input) {
    if (input instanceof Uint8Array) {
      return Hash.hashBinary('SHA256', input);
    }
    return RequestHash.getRequestHash(input.getNormalizedData());
  },

  parse : function(rd) {
    rd = rd.getObject(BaseProperties.REQUEST_HASH_JSON);
    if (rd.getString(Jcs.ALGORITHM_JSON) != RequestHash.JOSE_SHA_256_ALG_ID) {
      throw new TypeError('Expected algorithm: ' + RequestHash.JOSE_SHA_256_ALG_ID);
    }
    return rd.getBinary(Jcs.VALUE_JSON);
  }

};


module.exports = RequestHash;
