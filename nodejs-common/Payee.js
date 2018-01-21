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

// Payee object

const JsonUtil = require('webpki.org').JsonUtil;

const BaseProperties = require('./BaseProperties');

function Payee(rd) {
  this.commonName = rd.getString(BaseProperties.COMMON_NAME_JSON);
  this.id = rd.getString(BaseProperties.ID_JSON);
}

Payee.init = function(commonName, id) {
  return new Payee(new JsonUtil.ObjectReader(new JsonUtil.ObjectWriter()
    .setString(BaseProperties.COMMON_NAME_JSON, commonName)
    .setString(BaseProperties.ID_JSON, id).getRootObject()));
};

Payee.prototype.writeObject = function() {
  return new JsonUtil.ObjectWriter()
    .setString(BaseProperties.COMMON_NAME_JSON, this.commonName)
    .setString(BaseProperties.ID_JSON, this.id);
};

Payee.prototype.getCommonName = function() {
  return this.commonName;
};

Payee.prototype.getId = function() {
  return this.id;
};


module.exports = Payee;
